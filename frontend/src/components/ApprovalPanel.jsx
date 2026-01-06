import api from "../services/api";
import { useState } from "react";
import { isAdmin } from "../services/auth"; 

{isAdmin() && (
  <button onClick={() => transition("APPROVED")}>Approve</button>
)}

export default function ApprovalPanel({ scenario, onUpdate }) {
  const [comment, setComment] = useState("");

  const transition = async (status) => {
    await api.post(
      `/scenarios/${scenario.id}/approval`,
      { targetStatus: status, comment },
      { headers: { "X-Actor": "admin" } }
    );
    onUpdate();
  };

  return (
    <div className="border p-3 rounded">
      <p>Status: <b>{scenario.approvalStatus}</b></p>

      <textarea
        placeholder="Approval comment"
        value={comment}
        onChange={(e) => setComment(e.target.value)}
        className="border w-full mb-2"
      />

      <div className="flex gap-2">
        <button onClick={() => transition("STAGING")}>Stage</button>
        <button onClick={() => transition("APPROVED")}>Approve</button>
        <button onClick={() => transition("REJECTED")}>Reject</button>
      </div>
    </div>
  );
}
