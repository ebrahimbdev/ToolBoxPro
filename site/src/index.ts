/**
 * dastet.ir multi-app site.
 *
 * Routing by hostname:
 *   dastet.ir            -> /hub.html        (apps hub)
 *   toolbox.dastet.ir    -> /apps/toolbox.html
 *   <any>.dastet.ir      -> /apps/<name>.html, falls back to the hub
 *
 * To add a new app: drop public/apps/<name>.html in this project and
 * <name>.dastet.ir starts working after `npx wrangler deploy`.
 */
export interface Env {
	ASSETS: Fetcher;
}

const SUB = ".dastet.ir";

export default {
	async fetch(request: Request, env: Env): Promise<Response> {
		const host = new URL(request.url).hostname.toLowerCase().replace(/^www\./, "");
		const asset = (path: string): Promise<Response> =>
			env.ASSETS.fetch(new Request(new URL(path, request.url), request));

		if (host === "dastet.ir") {
			// Ad pages must never fall back to the hub: a missing /ads/ file is a 404
			// (the WebView gate treats it as "ad unavailable" instead of showing the site).
			const path = new URL(request.url).pathname;
			if (path.startsWith("/ads/")) return new Response("Not found", { status: 404 });
			return asset("/hub.html");
		}

		if (host.endsWith(SUB)) {
			const sub = host.slice(0, -SUB.length).replace(/[^a-z0-9-]/g, "");
			if (sub) {
				const page = await asset(`/apps/${sub}.html`);
				if (page.status !== 404) return page;
			}
		}
		return asset("/hub.html");
	},
} satisfies ExportedHandler<Env>;
