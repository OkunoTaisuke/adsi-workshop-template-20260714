"use client";

import { useAuth } from "@/lib/auth-context";
import { apiFetch } from "@/lib/api-client";
import { useEffect, useState } from "react";

interface Department {
  id: number;
  name: string;
}

interface EmployeeDetail {
  id: number;
  name: string;
  email: string;
  departmentName: string | null;
  departmentId: number | null;
  roles: string[];
}

const ROLE_OPTIONS = ["USER", "MANAGER", "ADMIN"];

export default function EmployeesPage() {
  const { employee, hasRole } = useAuth();
  const [employees, setEmployees] = useState<EmployeeDetail[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [formName, setFormName] = useState("");
  const [formEmail, setFormEmail] = useState("");
  const [formPassword, setFormPassword] = useState("");
  const [formDeptId, setFormDeptId] = useState<number | "">("");
  const [formRoles, setFormRoles] = useState<string[]>(["USER"]);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    if (!hasRole("ADMIN")) return;
    let active = true;
    async function loadData() {
      try {
        const [emps, depts] = await Promise.all([
          apiFetch<EmployeeDetail[]>("/employees"),
          apiFetch<Department[]>("/departments"),
        ]);
        if (active) {
          setEmployees(emps);
          setDepartments(depts);
        }
      } catch {
        /* ignore */
      }
    }
    loadData();
    return () => { active = false; };
  }, [refreshKey, hasRole]);

  const handleRoleToggle = (role: string) => {
    setFormRoles((prev) =>
      prev.includes(role) ? prev.filter((r) => r !== role) : [...prev, role]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (!formDeptId) {
      setError("部署を選択してください");
      return;
    }
    if (formRoles.length === 0) {
      setError("ロールを1つ以上選択してください");
      return;
    }
    try {
      await apiFetch("/employees", {
        method: "POST",
        body: JSON.stringify({
          name: formName,
          email: formEmail,
          password: formPassword,
          departmentId: formDeptId,
          roles: formRoles,
        }),
      });
      setShowForm(false);
      setFormName("");
      setFormEmail("");
      setFormPassword("");
      setFormDeptId("");
      setFormRoles(["USER"]);
      setRefreshKey((k) => k + 1);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "エラーが発生しました");
    }
  };

  if (!employee || !hasRole("ADMIN")) {
    return <div className="p-6 text-gray-500">アクセス権がありません</div>;
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-bold">社員管理</h1>
        <button
          onClick={() => setShowForm(true)}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
        >
          + 新規登録
        </button>
      </div>

      {showForm && (
        <div className="mb-6 p-4 border rounded bg-gray-50">
          <form onSubmit={handleSubmit} className="space-y-3">
            <div>
              <label className="block text-sm font-medium text-gray-700">氏名</label>
              <input type="text" value={formName} onChange={(e) => setFormName(e.target.value)} required className="mt-1 w-full px-3 py-2 border rounded text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">メールアドレス</label>
              <input type="email" value={formEmail} onChange={(e) => setFormEmail(e.target.value)} required className="mt-1 w-full px-3 py-2 border rounded text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">パスワード</label>
              <input type="password" value={formPassword} onChange={(e) => setFormPassword(e.target.value)} required minLength={8} className="mt-1 w-full px-3 py-2 border rounded text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">所属部署</label>
              <select value={formDeptId} onChange={(e) => setFormDeptId(Number(e.target.value))} required className="mt-1 w-full px-3 py-2 border rounded text-sm">
                <option value="">選択してください</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">ロール</label>
              <div className="flex gap-4 mt-1">
                {ROLE_OPTIONS.map((role) => (
                  <label key={role} className="flex items-center gap-1 text-sm">
                    <input
                      type="checkbox"
                      checked={formRoles.includes(role)}
                      onChange={() => handleRoleToggle(role)}
                    />
                    {role}
                  </label>
                ))}
              </div>
            </div>
            {error && <p className="text-red-600 text-sm">{error}</p>}
            <div className="flex gap-2">
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm">
                登録
              </button>
              <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400 text-sm">
                キャンセル
              </button>
            </div>
          </form>
        </div>
      )}

      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="border-b bg-gray-50">
            <th className="px-3 py-2 text-left">名前</th>
            <th className="px-3 py-2 text-left">メール</th>
            <th className="px-3 py-2 text-left">部署</th>
            <th className="px-3 py-2 text-left">ロール</th>
          </tr>
        </thead>
        <tbody>
          {employees.map((emp) => (
            <tr key={emp.id} className="border-b">
              <td className="px-3 py-2">{emp.name}</td>
              <td className="px-3 py-2">{emp.email}</td>
              <td className="px-3 py-2">{emp.departmentName || "-"}</td>
              <td className="px-3 py-2">{emp.roles.join(", ")}</td>
            </tr>
          ))}
          {employees.length === 0 && (
            <tr>
              <td colSpan={4} className="px-3 py-4 text-center text-gray-500">
                社員がいません
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
