import type { NextConfig } from "next";

const isSageMaker = process.env.SAGEMAKER === "1";

const nextConfig: NextConfig = {
  basePath: isSageMaker ? "/absports/3000" : undefined,
  allowedDevOrigins: ["juwddmach153dlz.studio.ap-northeast-1.sagemaker.aws"],
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
    ];
  },
};

export default nextConfig;
