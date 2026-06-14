config.devServer = config.devServer || {};
config.devServer.proxy = [
    {
        context: ["/api"],
        target: "http://91.207.75.72:8080",
        changeOrigin: true,
    },
];
