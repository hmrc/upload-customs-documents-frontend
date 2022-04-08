const path = require('path');

module.exports = function (env) {
  return {
    mode: 'production',
    optimization: {
      minimize: true,
      concatenateModules: true
    },
    watch: false,
    devtool: 'source-map',
    resolve: {
      extensions: ['.js', '.ts'],
      alias: {
        'node_modules': path.join(__dirname, 'node_modules'),
        'webjars': env.webjars.path
      }
    },
    module: {
      rules: [
        {
          test: /\.ts$/,
          exclude: /node_modules/,
          use: {
            loader: 'babel-loader',
            options: {
              presets: [
                '@babel/typescript',
                '@babel/preset-env'
              ],
              plugins: [
                '@babel/plugin-proposal-class-properties'
              ]
            }
          }
        }
      ]
    },
    output: env.output
  }
};
