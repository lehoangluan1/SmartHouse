import "./SettingsSection.css";

function SettingsSection({ index, title, description, children }) {
  return (
    <section className="settings-section">
      <div className="settings-section__header">
        <h2 className="settings-section__title">
          <span>{index}</span> <span>{title}</span>
        </h2>

        {description ? (
          <p className="settings-section__description">{description}</p>
        ) : null}
      </div>

      <div className="settings-section__body">{children}</div>
    </section>
  );
}

export default SettingsSection;