"use client";

import { useAuth } from "@/lib/auth-context";
import { Header } from "@/components/Header";
import { ClockButtons } from "@/components/attendance/ClockButtons";
import { TodayStatus } from "@/components/attendance/TodayStatus";
import { useEffect, useState } from "react";
import * as api from "@/lib/attendance-api";
import type { AttendanceResponse } from "@/lib/types";
import Link from "next/link";
import { useEffect } from "react";

export default function Dashboard() {
  const { employee, isLoading } = useAuth();
  const [todayAttendance, setTodayAttendance] =
    useState<AttendanceResponse | null>(null);
  const [loadingToday, setLoadingToday] = useState(true);

  const [todayError, setTodayError] = useState<string | null>(null);

  useEffect(() => {
    if (!employee) return;
    let cancelled = false;

    api
      .getToday()
      .then((data) => {
        if (!cancelled) setTodayAttendance(data);
      })
      .catch(() => {
        if (!cancelled) setTodayError("勤怠状態の取得に失敗しました");
      })
      .finally(() => {
        if (!cancelled) setLoadingToday(false);
      });

    return () => {
      cancelled = true;
    };
  }, [employee]);

  useEffect(() => {
    if (!isLoading && !employee) {
      const loginPath = window.location.pathname.replace(/\/$/, "") + "/login";
      window.location.href = loginPath;
    }
  }, [isLoading, employee]);

  useEffect(() => {
    if (!isLoading && !employee) {
      const loginPath = window.location.pathname.replace(/\/$/, "") + "/login";
      window.location.href = loginPath;
    }
  }, [isLoading, employee]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-500">読み込み中...</p>
      </div>
    );
  }

  if (!employee) {
    return null;
  }

  const today = new Date();
  const dateStr = today.toLocaleDateString("ja-JP", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long",
  });

  const menuItems = [
    {
      title: "勤怠一覧",
      description: "自分の月別勤怠を確認",
      href: "/attendance",
      icon: "📋",
      available: true,
    },
    {
      title: "休暇申請",
      description: "有給・休暇の申請",
      href: "/leaves",
      icon: "📝",
      available: false,
    },
  ];

  const adminItems = [
    {
      title: "全社員勤怠",
      description: "全社員の勤怠一覧を確認",
      href: "/admin/attendance",
      icon: "👥",
      available: true,
    },
    {
      title: "休暇承認",
      description: "休暇申請の承認・却下",
      href: "/admin/leaves",
      icon: "✅",
      available: false,
    },
    {
      title: "月次レポート",
      description: "集計・CSV出力",
      href: "/admin/reports",
      icon: "📊",
      available: false,
    },
  ];

  return (
    <>
      <Header />
      <main className="flex-1 p-6 bg-gray-50">
        <div className="max-w-4xl mx-auto">
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900">
              ようこそ、{employee.name}さん
            </h2>
            <p className="text-gray-500 mt-1">{dateStr}</p>
          </div>

          <section className="mb-8 bg-white rounded-lg border p-6">
            <h3 className="text-lg font-semibold text-gray-700 mb-4">
              打刻
            </h3>
            {loadingToday ? (
              <p className="text-gray-400 text-sm">読み込み中...</p>
            ) : todayError ? (
              <p className="text-red-600 text-sm">{todayError}</p>
            ) : (
              <div className="space-y-4">
                <TodayStatus attendance={todayAttendance} />
                <ClockButtons
                  attendance={todayAttendance}
                  onUpdate={setTodayAttendance}
                />
              </div>
            )}
          </section>

          <section className="mb-8">
            <h3 className="text-lg font-semibold text-gray-700 mb-4">
              メニュー
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {menuItems.map((item) => (
                <MenuCard key={item.title} {...item} />
              ))}
            </div>
          </section>

          {employee.roles?.includes("ADMIN") && (
            <section>
              <h3 className="text-lg font-semibold text-gray-700 mb-4">
                管理者メニュー
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {adminItems.map((item) => (
                  <MenuCard key={item.title} {...item} />
                ))}
              </div>
            </section>
          )}
        </div>
      </main>
    </>
  );
}

interface MenuCardProps {
  title: string;
  description: string;
  href: string;
  icon: string;
  available: boolean;
}

function MenuCard({ title, description, href, icon, available }: MenuCardProps) {
  const content = (
    <div
      className={`p-5 bg-white rounded-lg border shadow-sm ${
        available
          ? "border-gray-200 hover:border-blue-300 hover:shadow-md cursor-pointer"
          : "border-gray-100 opacity-60"
      } transition-all`}
    >
      <div className="text-2xl mb-2">{icon}</div>
      <h4 className="font-semibold text-gray-900">{title}</h4>
      <p className="text-sm text-gray-500 mt-1">{description}</p>
      {!available && (
        <span className="inline-block mt-2 text-xs text-gray-400 bg-gray-50 px-2 py-0.5 rounded">
          準備中
        </span>
      )}
    </div>
  );

  if (available) {
    return <Link href={href}>{content}</Link>;
  }
  return content;
}
