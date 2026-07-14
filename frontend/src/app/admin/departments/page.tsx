"use client";

import { useAuth } from "@/lib/auth-context";
import { apiFetch } from "@/lib/api-client";
import { useEffect, useState } from "react";

interface Department {
  id: number;
  name: string;
}

export default function DepartmentsPage() {
  const { employee, hasRole } = useAuth();
  const [departments, setDepartments] = useState<Department[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    if (!hasRole("ADMIN")) return;
    let active = true;
    async function loadDepartments() {
      try {
        const data = await apiFetch<Department[]>("/departments");
        if (active) setDepartments(data);
      } catch {
        /* ignore */
      }
    }
    loadDepartments();
    return () => { active = false; };
  }, [refreshKey, hasRole]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      if (editId) {
        await apiFetch(`/departments/${editId}`, {
          method: "PUT",
          body: JSON.stringify({ name }),
        });
      } else {
        await apiFetch("/departments", {
          method: "POST",
          body: JSON.stringify({ name }),
        });
      }
      setShowForm(false);
      setEditId(null);
      setName("");
      setRefreshKey((k) => k + 1);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "エラーが発生しました");
    }
  };

  const handleEdit = (dept: Department) => {
    setEditId(dept.id);
    setName(dept.name);
    setShowForm(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await apiFetch(`/departments/${id}`, { method: "DELETE" });
      setRefreshKey((k) => k + 1);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : "削除に失敗しました");
    }
  };

  if (!employee || !hasRole("ADMIN")) {
    return <div className="p-6 text-gray-500">アクセス権がありません</div>;
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-bold">部署管理</h1>
        <button
          onClick={() => { setShowForm(true); setEditId(null); setName(""); }}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
        >
          + 新規追加
        </button>
      </div>

      {showForm && (
        <div className="mb-6 p-4 border rounded bg-gray-50">
          <form onSubmit={handleSubmit} className="space-y-3">
            <div>
              <label className="block text-sm font-medium text-gray-700">部署名</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                maxLength={100}
                className="mt-1 w-full px-3 py-2 border rounded text-sm"
              />
            </div>
            {error && <p className="text-red-600 text-sm">{error}</p>}
            <div className="flex gap-2">
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm">
                {editId ? "更新" : "作成"}
              </button>
              <button type="button" onClick={() => { setShowForm(false); setEditId(null); }} className="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400 text-sm">
                キャンセル
              </button>
            </div>
          </form>
        </div>
      )}

      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="border-b bg-gray-50">
            <th className="px-3 py-2 text-left">部署名</th>
            <th className="px-3 py-2 text-left">操作</th>
          </tr>
        </thead>
        <tbody>
          {departments.map((dept) => (
            <tr key={dept.id} className="border-b">
              <td className="px-3 py-2">{dept.name}</td>
              <td className="px-3 py-2 flex gap-2">
                <button onClick={() => handleEdit(dept)} className="text-blue-600 hover:text-blue-800 text-xs">
                  編集
                </button>
                <button onClick={() => handleDelete(dept.id)} className="text-red-600 hover:text-red-800 text-xs">
                  削除
                </button>
              </td>
            </tr>
          ))}
          {departments.length === 0 && (
            <tr>
              <td colSpan={2} className="px-3 py-4 text-center text-gray-500">
                部署がありません
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
