import { loadEnv } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';
import path from 'path';
// import viteCompression from 'vite-plugin-compression';
import uniReadPagesV3Plugin from './sheep/router/utils/uni-read-pages-v3';
import mpliveMainfestPlugin from './sheep/libs/mplive-manifest-plugin';


// https://vitejs.dev/config/
export default (command, mode) => {
	const env = loadEnv(mode, __dirname, 'SHOPRO_');
	return {
		define: {
			process: JSON.stringify({ env: { UNI_APP_X: 'false' } }),
		},
		resolve: {
			alias: {
				'@': path.resolve(__dirname, './')
			}
		},
		envPrefix: "SHOPRO_",
		css: {
			preprocessorOptions: {
				scss: {
					// HBuilderX 5.05 still invokes Dart Sass through its legacy JS API.
					// Project-owned styles use the module system and are guarded below.
					// DCloud's bundled uni_modules and compiler still use these retired
					// APIs, so acknowledge only the upstream transition categories.
					silenceDeprecations: [
						'legacy-js-api',
						'import',
						'global-builtin',
						'color-functions',
					],
				},
			},
		},
		plugins: [
			uni(),
			// viteCompression({
			// 	verbose: false
			// }),
			uniReadPagesV3Plugin({
				pagesJsonDir: path.resolve(__dirname, './pages.json'),
				includes: ['path', 'aliasPath', 'name', 'meta'],
			}),
			mpliveMainfestPlugin(env.SHOPRO_MPLIVE_ON)
		],
		server: {
			host: true,
			// open: true,
			port: env.SHOPRO_DEV_PORT,
			hmr: {
				overlay: true,
			},
		},
	};
};
