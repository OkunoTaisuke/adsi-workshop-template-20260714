import http from "node:http";

const LISTEN_PORT = 3000;
const NEXT_PORT = 3001;
const PREFIX = "/codeeditor/default/absports/3000";

const server = http.createServer((req, res) => {
  const url = PREFIX + req.url;
  console.log(`[proxy] ${req.method} ${req.url} -> ${url}`);

  const options = {
    hostname: "127.0.0.1",
    port: NEXT_PORT,
    path: url,
    method: req.method,
    headers: req.headers,
  };

  const proxyReq = http.request(options, (proxyRes) => {
    console.log(`[proxy]   <- ${proxyRes.statusCode}`);
    res.writeHead(proxyRes.statusCode, proxyRes.headers);
    proxyRes.pipe(res, { end: true });
  });

  proxyReq.on("error", (err) => {
    console.error("[proxy] upstream error:", err.message);
    res.writeHead(502);
    res.end("Bad Gateway");
  });

  req.pipe(proxyReq, { end: true });
});

server.listen(LISTEN_PORT, () => {
  console.log(`[sagemaker-proxy] :${LISTEN_PORT} -> :${NEXT_PORT} (prefix: ${PREFIX})`);
});
