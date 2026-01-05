import { useState } from "react";
import { rollbackScenario } from "../services/api";

export default function RollbackDialog({ scenarioId, onSuccess }) {
  const [version, setVersion] = useState("");

  const rollback = async () => {
    await rollbackScenario(scenarioId, Number(version));
    onSuccess();
  };

  return (
    <div className="p-4 border rounded">
      <h3 className="font-semibold mb-2">Rollback Scenario</h3>

      <input
        type="number"
        placeholder="Target version"
        value={version}
        onChange={(e) => setVersion(e.target.value)}
        className="border p-1 mr-2"
      />

      <button
        onClick={rollback}
        className="bg-red-600 text-white px-3 py-1 rounded"
      >
        Rollback
      </button>
    </div>
  );
}
