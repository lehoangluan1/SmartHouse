function ConfigListPanel({
    loading,
    configs = [],
    selectedConfigId,
    onSelectConfig,
    onCreateNew,
  }) {
    return (
      <section className="config-panel config-list-panel">
        <div className="config-panel__header">
          <h2>Configs</h2>
        </div>
  
        <div className="config-list">
          {loading ? <div className="config-empty">Loading configs...</div> : null}
  
          {!loading &&
            configs.map((config) => {
              const isSelected = config.id === selectedConfigId;
  
              return (
                <button
                  key={config.id}
                  type="button"
                  className={`config-list-item ${isSelected ? "is-selected" : ""}`}
                  onClick={() => onSelectConfig(config)}
                >
                  <div className="config-list-item__left">
                    <div className="config-list-item__avatar">
                      <svg viewBox="0 0 24 24" fill="none">
                        <path
                          d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 7a7 7 0 1 1 14 0"
                          stroke="currentColor"
                          strokeWidth="1.7"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                    </div>
  
                    <div className="config-list-item__content">
                      <div className="config-list-item__title-row">
                        <span className="config-list-item__title">{config.name}</span>
                        <span className="config-list-item__owner">
                          ({config.ownerName || config.owner || "System"})
                        </span>
                      </div>
  
                      <div className="config-list-item__meta">
                        {config.active ? (
                          <span className="config-list-item__status">
                            <span className="config-list-item__status-dot" />
                            Active
                          </span>
                        ) : null}
                        <span>Config</span>
                      </div>
                    </div>
                  </div>
  
                  <span className="config-list-item__edit">Edit</span>
                </button>
              );
            })}
  
          <button type="button" className="config-create-button" onClick={onCreateNew}>
            <span className="config-create-button__plus">+</span>
            <span>Create New Config</span>
          </button>
        </div>
      </section>
    );
  }
  
  export default ConfigListPanel;