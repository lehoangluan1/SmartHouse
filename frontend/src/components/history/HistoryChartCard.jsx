import "./HistoryChartCard.css";

function HistoryChartCard({ item }) {
  return (
    <article className="history-chart-card">
      <div className="history-chart-card__header">
        <div className="history-chart-card__title-wrap">
          <span className={`history-chart-card__icon ${item.colorTone}`}>
            {renderHistoryIcon(item.icon)}
          </span>
          <h3>{item.name}</h3>
        </div>

        <span className={`history-chart-card__badge ${item.badgeTone}`}>
          {item.badge}
        </span>
      </div>

      <div className="history-chart-card__chart">
        <MiniLineChart
          points={item.points}
          thresholds={item.thresholds}
          colorTone={item.colorTone}
          lineType={item.lineType}
          yLabels={item.yLabels}
        />
      </div>

      <div className="history-chart-card__stats">
        {(item.stats || []).map((stat) => (
          <div key={stat.label} className="history-chart-card__stat">
            <div className="history-chart-card__stat-label">{stat.label}</div>
            <div className={`history-chart-card__stat-value ${item.colorTone}`}>
              {stat.value}
            </div>
          </div>
        ))}
      </div>
    </article>
  );
}

function MiniLineChart({
  points,
  thresholds = [],
  colorTone = "blue",
  lineType = "line",
  yLabels,
}) {
  const width = 420;
  const height = 220;
  const padding = { top: 16, right: 96, bottom: 34, left: 34 };
  const innerWidth = width - padding.left - padding.right;
  const innerHeight = height - padding.top - padding.bottom;

  if (!points?.length) {
    return <div className="history-chart-card__empty">No telemetry data</div>;
  }

  const thresholdValues = thresholds.map((t) => t.value);
  const values = points.map((item) => item.value);
  const minValue = yLabels ? 0 : Math.min(...values, ...thresholdValues);
  const maxValue = yLabels ? 1 : Math.max(...values, ...thresholdValues);

  const safeMin = minValue === maxValue ? minValue - 1 : minValue;
  const safeMax = minValue === maxValue ? maxValue + 1 : maxValue;

  const xForIndex = (index) => {
    if (points.length <= 1) return padding.left;
    return padding.left + (index / (points.length - 1)) * innerWidth;
  };

  const yForValue = (value) => {
    const ratio = (value - safeMin) / (safeMax - safeMin);
    return padding.top + innerHeight - ratio * innerHeight;
  };

  const polylinePoints =
    lineType === "step"
      ? buildStepPoints(points, xForIndex, yForValue)
      : points
          .map((point, index) => `${xForIndex(index)},${yForValue(point.value)}`)
          .join(" ");

  const xTickIndexes = buildTickIndexes(points.length, 8);
  const gridLines = 4;

  const thresholdItems = buildThresholdLayout(
    thresholds,
    yForValue,
    padding.top,
    padding.top + innerHeight
  );

  const thresholdLineEndX = padding.left + innerWidth;
  const thresholdConnectorStartX = thresholdLineEndX + 4;
  const thresholdTextX = thresholdLineEndX + 12;

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="history-chart-card__svg">
      {Array.from({ length: gridLines + 1 }).map((_, index) => {
        const y = padding.top + (innerHeight / gridLines) * index;
        return (
          <line
            key={`h-${index}`}
            x1={padding.left}
            y1={y}
            x2={padding.left + innerWidth}
            y2={y}
            className="history-chart-grid-line"
          />
        );
      })}

      {xTickIndexes.map((index) => {
        const x = xForIndex(index);
        return (
          <line
            key={`v-${index}`}
            x1={x}
            y1={padding.top}
            x2={x}
            y2={padding.top + innerHeight}
            className="history-chart-grid-line"
          />
        );
      })}

      {thresholdItems.map((threshold) => (
        <g key={threshold.label}>
          <line
            x1={padding.left}
            y1={threshold.lineY}
            x2={thresholdLineEndX}
            y2={threshold.lineY}
            className={`history-threshold-line ${threshold.tone}`}
          />

          <path
            d={`M ${thresholdLineEndX} ${threshold.lineY} 
                L ${thresholdConnectorStartX} ${threshold.lineY}
                L ${thresholdTextX - 4} ${threshold.textY}`}
            className={`history-threshold-connector ${threshold.tone}`}
            fill="none"
          />

          <text
            x={thresholdTextX}
            y={threshold.textY}
            textAnchor="start"
            dominantBaseline="middle"
            className={`history-threshold-text ${threshold.tone}`}
          >
            {threshold.label}
          </text>
        </g>
      ))}

      <line
        x1={padding.left}
        y1={padding.top}
        x2={padding.left}
        y2={padding.top + innerHeight}
        className="history-chart-axis"
      />
      <line
        x1={padding.left}
        y1={padding.top + innerHeight}
        x2={padding.left + innerWidth}
        y2={padding.top + innerHeight}
        className="history-chart-axis"
      />

      <polyline
        fill="none"
        points={polylinePoints}
        className={`history-chart-polyline ${colorTone}`}
      />

      {xTickIndexes.map((index) => {
        const x = xForIndex(index);
        return (
          <text
            key={`label-${index}`}
            x={x}
            y={height - 8}
            textAnchor="middle"
            className="history-chart-x-label"
          >
            {points[index]?.label || "--:--"}
          </text>
        );
      })}

      {yLabels ? (
        <>
          <text
            x={8}
            y={padding.top + innerHeight}
            className="history-chart-y-label"
          >
            {yLabels[0]}
          </text>
          <text
            x={8}
            y={padding.top + 8}
            className="history-chart-y-label"
          >
            {yLabels[1]}
          </text>
        </>
      ) : (
        <>
          <text
            x={8}
            y={padding.top + innerHeight}
            className="history-chart-y-label"
          >
            {Math.round(safeMin)}
          </text>
          <text
            x={8}
            y={padding.top + innerHeight / 2}
            className="history-chart-y-label"
          >
            {Math.round((safeMin + safeMax) / 2)}
          </text>
          <text
            x={8}
            y={padding.top + 8}
            className="history-chart-y-label"
          >
            {Math.round(safeMax)}
          </text>
        </>
      )}
    </svg>
  );
}

function buildThresholdLayout(thresholds, yForValue, minY, maxY) {
  if (!thresholds?.length) return [];

  const minGap = 16;

  const items = thresholds
    .map((threshold) => ({
      ...threshold,
      lineY: yForValue(threshold.value),
    }))
    .sort((a, b) => a.lineY - b.lineY)
    .map((item) => ({
      ...item,
      textY: item.lineY,
    }));

  for (let index = 1; index < items.length; index += 1) {
    if (items[index].textY - items[index - 1].textY < minGap) {
      items[index].textY = items[index - 1].textY + minGap;
    }
  }

  if (items.length > 0 && items[items.length - 1].textY > maxY - 6) {
    items[items.length - 1].textY = maxY - 6;

    for (let index = items.length - 2; index >= 0; index -= 1) {
      if (items[index + 1].textY - items[index].textY < minGap) {
        items[index].textY = items[index + 1].textY - minGap;
      }
    }
  }

  if (items.length > 0 && items[0].textY < minY + 6) {
    items[0].textY = minY + 6;

    for (let index = 1; index < items.length; index += 1) {
      if (items[index].textY - items[index - 1].textY < minGap) {
        items[index].textY = items[index - 1].textY + minGap;
      }
    }
  }

  return items;
}

function buildStepPoints(points, xForIndex, yForValue) {
  if (!points.length) return "";

  const segments = [];
  const firstX = xForIndex(0);
  const firstY = yForValue(points[0].value);
  segments.push(`${firstX},${firstY}`);

  for (let index = 1; index < points.length; index += 1) {
    const currentX = xForIndex(index);
    const prevY = yForValue(points[index - 1].value);
    const currentY = yForValue(points[index].value);

    segments.push(`${currentX},${prevY}`);
    segments.push(`${currentX},${currentY}`);
  }

  return segments.join(" ");
}

function buildTickIndexes(total, tickCount) {
  if (total <= tickCount) {
    return Array.from({ length: total }, (_, index) => index);
  }

  const step = (total - 1) / (tickCount - 1);
  const indexes = [];

  for (let index = 0; index < tickCount; index += 1) {
    indexes.push(Math.round(index * step));
  }

  return Array.from(new Set(indexes));
}

function renderHistoryIcon(type) {
  switch (type) { 
    case "temperature":
      return (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M12 4v9.5a3.5 3.5 0 1 0 2 3.16V4a2 2 0 1 0-4 0v12.66A3.5 3.5 0 1 0 12 13.5"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "humidity":
      return (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M12 3s-5 5.34-5 9a5 5 0 0 0 10 0c0-3.66-5-9-5-9Z"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinejoin="round"
          />
          <path
            d="M8 17c.8.7 1.9 1 3 1"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
        </svg>
      );
    case "light":
    case "bulb":
      return (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M9 18h6M10 21h4M8 10a4 4 0 1 1 8 0c0 1.7-.8 2.7-1.8 3.8-.7.8-1.2 1.6-1.2 2.2h-2c0-.6-.5-1.4-1.2-2.2C8.8 12.7 8 11.7 8 10Z"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "fan":
      return (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M12 12m-1.5 0a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0"
            stroke="currentColor"
            strokeWidth="1.8"
          />
          <path
            d="M12 4c1.8 0 2.8 2.2 1.7 3.7L12 10M20 12c0 1.8-2.2 2.8-3.7 1.7L14 12M12 20c-1.8 0-2.8-2.2-1.7-3.7L12 14M4 12c0-1.8 2.2-2.8 3.7-1.7L10 12"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    default:
      return null;
  }
}

export default HistoryChartCard;