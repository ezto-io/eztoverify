# Documentation

Product documentation for the Verify platform — a [Docusaurus](https://docusaurus.io/)
site under `verify/` (workflows, integration, API, developer tools, changelogs).

## Run locally

From `verify/`:

```bash
./start.sh             # install (first run) + dev server, live reload, http://localhost:3000
./start.sh build       # build static bundle into ./build, then serve it
PORT=3100 ./start.sh   # override port
```

Or use npm directly:

```bash
npm install            # first time only
npm run start          # dev server
npm run build          # static bundle in ./build
npm run serve          # serve the built bundle
```

## Node version

Docusaurus 2.1.0 needs **Node >=16 <21** (pinned in `package.json` engines; `.nvmrc` -> 20).
Node 21+ breaks the build (`document is not defined`).

Use [**nvm**](https://github.com/nvm-sh/nvm) to manage the Node version. Install:
`curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash`, then from `verify/`:

```bash
nvm install   # reads .nvmrc -> 20
nvm use
```

> Broken-link handling is set to `warn` (not `fail`), so the site still builds if a
> page is removed. After editing `sidebars.js`, run `npm run build` and check the
> console for broken-link warnings.
