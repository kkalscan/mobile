config.devServer = config.devServer || {};
// Local stub API (VISION_PROVIDER=stub). Run: PORT=9090 VISION_PROVIDER=stub ./gradlew :run in kkalscanbackend
config.devServer.proxy = [
    {
        context: ["/api"],
        target: "http://127.0.0.1:9090",
        changeOrigin: true,
    },
];
