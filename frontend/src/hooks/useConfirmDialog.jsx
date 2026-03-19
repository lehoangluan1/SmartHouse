import { useCallback, useState } from "react";

export default function useConfirmDialog() {
  const [dialogState, setDialogState] = useState({
    open: false,
    title: "",
    message: "",
    confirmText: "Confirm",
    cancelText: "Cancel",
    tone: "danger",
    resolve: null,
  });

  const confirm = useCallback((options = {}) => {
    return new Promise((resolve) => {
      setDialogState({
        open: true,
        title: options.title || "Confirm",
        message: options.message || "Are you sure?",
        confirmText: options.confirmText || "Confirm",
        cancelText: options.cancelText || "Cancel",
        tone: options.tone || "danger",
        resolve,
      });
    });
  }, []);

  const closeWithResult = useCallback((result) => {
    setDialogState((prev) => {
      prev.resolve?.(result);

      return {
        ...prev,
        open: false,
        resolve: null,
      };
    });
  }, []);

  const handleConfirm = useCallback(() => {
    closeWithResult(true);
  }, [closeWithResult]);

  const handleCancel = useCallback(() => {
    closeWithResult(false);
  }, [closeWithResult]);

  return {
    confirm,
    dialogState,
    handleConfirm,
    handleCancel,
  };
}