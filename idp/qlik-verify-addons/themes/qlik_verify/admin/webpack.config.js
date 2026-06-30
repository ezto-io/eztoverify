const path = require('path');
const CopyPlugin = require("copy-webpack-plugin");
const ReplaceInFileWebpackPlugin = require('replace-in-file-webpack-plugin');

module.exports = {
  mode: 'production',
  entry: './resources/js/custom/curl_converter.js',
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader'
        }
      }
    ]
  },
  resolve: {
    fallback: {
      "fs": false,
      "path": require.resolve("path-browserify")
    }
  },
  experiments: {
    topLevelAwait: true
  },
  output: {
    filename: 'curl_converter.js',
    libraryTarget: 'var',
    library: 'curl_converter'
  },
  plugins: [
    new CopyPlugin({
      patterns: [
        'node_modules/web-tree-sitter/tree-sitter.wasm',
        'node_modules/curlconverter/dist/tree-sitter-bash.wasm',
      ]
    }),
    new ReplaceInFileWebpackPlugin([
      {
        dir: 'dist',
        files: ['curl_converter.js'],
        rules: [
          {
            search: /\"\/tree-sitter-bash.wasm\"/,
            replace: `window.location.protocol +'//'+ window.location.host + '/auth/tree-sitter-bash.wasm'`
          },
          {
            search: /tree-sitter\.wasm/,
            replace: "./tree-sitter.wasm"
          }]
      }
    ])
  ]
};
