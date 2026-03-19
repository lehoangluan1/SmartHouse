import { useEffect, useMemo, useState } from "react";
import "./AuditLogsPage.css";
import AuditSectionCard from "../../components/audit/AuditSectionCard";
import AuditSearchBar from "../../components/audit/AuditSearchBar";
import AuditFilterTabs from "../../components/audit/AuditFilterTabs";
import ConfigChangesTable from "../../components/audit/ConfigChangesTable";
import EventHistoryTable from "../../components/audit/EventHistoryTable";
import AuditPagination from "../../components/audit/AuditPagination";
import { fetchAuditDashboard } from "../../api/auditApi";
import { useAuth } from "../../providers/AuthProvider";
const EVENT_TABS = [
  { label: "All", value: "all" },
  { label: "Alerts", value: "alerts" },
  { label: "Device Events", value: "device" },
  { label: "System", value: "system" },
];

const DEFAULT_SUMMARY = {
  alerts: 0,
  device: 0,
  system: 0,
  totalEvents: 0,
  configChanges: 0,
};

const DEFAULT_PAGE = {
  items: [],
  page: 0,
  size: 10,
  totalItems: 0,
  totalPages: 0,
  first: true,
  last: true,
};

function normalizeBooleanLike(value) {
  if (value === true || value === "true") return "ON";
  if (value === false || value === "false") return "OFF";
  return value;
}

function normalizeText(value, fallback = "-") {
  if (value === null || value === undefined) return fallback;
  const normalized = normalizeBooleanLike(value);
  const text = String(normalized).trim();
  return text ? text : fallback;
}

function normalizeType(type) {
  const value = normalizeText(type, "SYSTEM_EVENT").toUpperCase();

  if (value === "MANUAL_CONTROL") return "MANUAL_CONTROL";
  if (value === "MANUAL_HOLD_STARTED") return "MANUAL_HOLD_STARTED";
  if (value === "MANUAL_HOLD_RESTORED") return "MANUAL_HOLD_RESTORED";
  if (value === "MANUAL_HOLD_CLEARED") return "MANUAL_HOLD_CLEARED";
  if (value === "POWER") return "POWER";
  if (value === "MODE") return "MODE";
  if (value === "LIGHT") return "LIGHT";
  if (value === "FAN") return "FAN";

  return value;
}

function normalizeState(value) {
  return normalizeText(normalizeBooleanLike(value), "-");
}

function inferDeviceLabel(deviceName, type) {
  const raw = String(deviceName || "").trim();
  const upperName = raw.toUpperCase();
  const upperType = String(type || "").toUpperCase();

  if (upperName.includes("LIGHT")) return "Light";
  if (upperName.includes("FAN")) return "Fan";
  if (upperName.includes("MODE")) return "Mode";
  if (upperType === "LIGHT") return "Light";
  if (upperType === "FAN") return "Fan";
  if (upperType === "POWER" && upperName) return raw;

  return raw || "-";
}

function normalizeDetails(item) {
  const details = normalizeText(item.details, "-");

  if (details === "-" && item.type === "MODE") return "Mode changed";
  if (details === "-" && item.type === "POWER") return "Power changed";

  return details;
}

function formatDateTimeParts(value) {
  if (!value) return { time: "-", date: "-" };

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return { time: "-", date: "-" };
  }

  return {
    time: date.toLocaleTimeString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
    }),
    date: date.toLocaleDateString("vi-VN"),
  };
}

function normalizeEvent(item) {
  const type = normalizeType(item.type);
  const fromState = normalizeState(item.fromState);
  const toState = normalizeState(item.toState);
  const deviceName = normalizeText(item.deviceName, "-");
  const deviceLabel = inferDeviceLabel(deviceName, type);

  return {
    ...item,
    type,
    category: normalizeText(item.category, "system").toLowerCase(),
    status: normalizeText(item.status, "LOGGED").toUpperCase(),
    fromState,
    toState,
    details: normalizeDetails({
      ...item,
      type,
      fromState,
      toState,
    }),
    username: normalizeText(item.username, "System"),
    deviceName,
    deviceLabel,
  };
}

function normalizeConfigChange(item) {
  const { time, date } = formatDateTimeParts(item.createdAt);

  return {
    ...item,
    time,
    date,
    user: normalizeText(item.user || item.username, "System"),
    username: normalizeText(item.username, "System"),
    deviceName: normalizeText(item.deviceName, "-"),
    prevConfig: item.prevConfig ?? "-",
    newConfig: item.newConfig ?? "-",
    reason: item.reason ?? "-",
  };
}

function buildDefaultRange() {
  const to = new Date();
  const from = new Date();
  from.setDate(to.getDate() - 7);

  return {
    from: from.toISOString(),
    to: to.toISOString(),
  };
}

function toServerPageData(raw, mapper, fallbackSize) {
  const resolved = raw || DEFAULT_PAGE;
  const items = Array.isArray(resolved.items) ? resolved.items.map(mapper) : [];

  return {
    ...DEFAULT_PAGE,
    ...resolved,
    size: resolved.size || fallbackSize,
    items,
  };
}

function AuditLogsPage() {
  const { user: currentUser } = useAuth();
  const homeId = currentUser?.homeId ? Number(currentUser.homeId) : null;

  const [summary, setSummary] = useState(DEFAULT_SUMMARY);
  const [configPageData, setConfigPageData] = useState({
    ...DEFAULT_PAGE,
    size: 10,
  });
  const [eventPageData, setEventPageData] = useState({
    ...DEFAULT_PAGE,
    size: 20,
  });

  const [loadingConfig, setLoadingConfig] = useState(true);
  const [loadingEvents, setLoadingEvents] = useState(true);
  const [error, setError] = useState("");

  const [configKeyword, setConfigKeyword] = useState("");
  const [eventKeyword, setEventKeyword] = useState("");
  const [activeTab, setActiveTab] = useState("all");

  const [configPage, setConfigPage] = useState(0);
  const [eventPage, setEventPage] = useState(0);

  const range = useMemo(() => buildDefaultRange(), []);

  useEffect(() => {
    setConfigPage(0);
  }, [configKeyword]);

  useEffect(() => {
    setEventPage(0);
  }, [eventKeyword, activeTab]);

  useEffect(() => {
    loadConfigChanges();
  }, [homeId, configPage, configKeyword, range.from, range.to]);

  useEffect(() => {
    loadEventsAndSummary();
  }, [homeId, eventPage, eventKeyword, activeTab, range.from, range.to]);

  async function loadConfigChanges() {
    try {
      setLoadingConfig(true);
      setError("");

      const res = await fetchAuditDashboard({
        homeId,
        from: range.from,
        to: range.to,
        configPage,
        configSize: 10,
        configKeyword,
        eventPage: 0,
        eventSize: 1,
        eventKeyword: "",
        eventCategory: "all",
      });
      console.log(res);
      setConfigPageData(
        toServerPageData(res?.configChanges, normalizeConfigChange, 10)
      );
    } catch (err) {
      setError(err.message || "Unable to load config changes data");
    } finally {
      setLoadingConfig(false);
    }
  }

  async function loadEventsAndSummary() {
    try {
      setLoadingEvents(true);
      setError("");

      const res = await fetchAuditDashboard({
        homeId,
        from: range.from,
        to: range.to,
        configPage: 0,
        configSize: 1,
        configKeyword: "",
        eventPage,
        eventSize: 20,
        eventKeyword,
        eventCategory: activeTab,
      });

      setEventPageData(toServerPageData(res?.events, normalizeEvent, 20));
      setSummary(res?.summary || DEFAULT_SUMMARY);
    } catch (err) {
      setError(err.message || "Unable to load event history data");
    } finally {
      setLoadingEvents(false);
    }
  }

  return (
    <div className="audit-page">
      <div className="audit-page__panel">
        <div className="audit-page__heading">
          <div className="audit-page__heading-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M8 7V5.5A1.5 1.5 0 0 1 9.5 4h5A1.5 1.5 0 0 1 16 5.5V7"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
              <rect
                x="5"
                y="7"
                width="14"
                height="13"
                rx="2.5"
                stroke="currentColor"
                strokeWidth="1.8"
              />
              <path
                d="M9 11H15M9 15H13"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
            </svg>
          </div>

          <div>
            <h1>System Logs &amp; Event History</h1>
            <p>Track alerts, device operations, and configuration updates.</p>
          </div>
        </div>

        {error ? <div className="audit-page__error">{error}</div> : null}

        <div className="audit-page__grid">
          <AuditSectionCard title="Active Config Changes" className="is-left">
            <AuditSearchBar
              value={configKeyword}
              onChange={setConfigKeyword}
              placeholder="Search user or config..."
            />

            {loadingConfig ? (
              <div className="audit-loading">Loading config changes...</div>
            ) : (
              <>
                <ConfigChangesTable rows={configPageData.items} />
                <AuditPagination
                  page={configPageData.page}
                  totalPages={configPageData.totalPages}
                  onChange={setConfigPage}
                />
              </>
            )}
          </AuditSectionCard>

          <AuditSectionCard title="Alerts & Event History" className="is-right">
            <div className="audit-toolbar">
              <AuditFilterTabs
                tabs={EVENT_TABS}
                activeTab={activeTab}
                onChange={setActiveTab}
              />

              <div className="audit-toolbar__search">
                <AuditSearchBar
                  value={eventKeyword}
                  onChange={setEventKeyword}
                  placeholder="Search event or details..."
                  compact
                />
              </div>
            </div>

            {loadingEvents ? (
              <div className="audit-loading">Loading audit logs...</div>
            ) : (
              <>
                <EventHistoryTable rows={eventPageData.items} />
                <AuditPagination
                  page={eventPageData.page}
                  totalPages={eventPageData.totalPages}
                  onChange={setEventPage}
                />
              </>
            )}

            <div className="audit-summary">
              <span>
                Alerts: <strong>{summary.alerts}</strong>
              </span>
              <span>
                Device Events: <strong>{summary.device}</strong>
              </span>
              <span>
                System: <strong>{summary.system}</strong>
              </span>
              <span>
                Config Changes: <strong>{summary.configChanges}</strong>
              </span>
            </div>
          </AuditSectionCard>
        </div>
      </div>
    </div>
  );
}

export default AuditLogsPage;