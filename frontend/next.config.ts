import type { NextConfig } from "next";

const isSageMaker = process.env.SAGEMAKER === "1";
const SAGEMAKER_BASE_PATH = "/codeeditor/default/absports/3000";

const nextConfig: NextConfig = {
  basePath: isSageMaker ? SAGEMAKER_BASE_PATH : undefined,
  assetPrefix: isSageMaker ? SAGEMAKER_BASE_PATH : undefined,
  skipTrailingSlashRedirect: isSageMaker ? true : undefined,
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
