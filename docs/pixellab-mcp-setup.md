# PixelLab MCP integration for GitHub Copilot coding agent

This repository uses the GitHub-hosted Copilot coding agent. To enable the PixelLab MCP tools you must run a local OpenAPI MCP server so the Authorization header can be added from a secret.

## 1) Add the API key as an environment secret in the `copilot` environment
- Settings → Environments → `copilot` → Add environment secret
- Name: `COPILOT_MCP_PIXELLAB`
- Value: your PixelLab API key

> Note: Secrets in the `copilot` environment are NOT automatically available as environment variables. They are only usable when mapped via the MCP configuration `env` block.

## 2) Ensure Node and npx are available for the agent
This PR adds `.github/copilot-setup-steps.yml` which installs Node.js if necessary. The coding agent runs these steps before starting MCP servers.

## 3) MCP configuration to paste into repo settings
Go to Settings → Copilot → Coding agent → MCP configuration, and paste the JSON below. This starts the generic OpenAPI MCP server and forwards to PixelLab with the Authorization header from the mapped secret.

````json
{
  "mcpServers": {
    "pixellab": {
      "type": "local",
      "command": "bash",
      "args": [
        "-lc",
        "npx -y @modelcontextprotocol/servers/openapi --spec https://api.pixellab.ai/mcp/openapi.json --name pixellab --header \"Authorization: Bearer ${PIXELLAB_API_KEY}\""
      ],
      "env": {
        "PIXELLAB_API_KEY": "COPILOT_MCP_PIXELLAB"
      },
      "tools": ["*"]
    }
  }
}
````

- If PixelLab uses a different auth header for your plan, change the header flag to:
  - `--header "x-api-key: ${PIXELLAB_API_KEY}"`
- If the OpenAPI spec URL differs, replace `https://api.pixellab.ai/mcp/openapi.json` with the exact spec URL from [PixelLab MCP docs](https://api.pixellab.ai/mcp/docs).

## 4) Verify in the Copilot agent session logs
1. Trigger the agent (for example, by assigning an issue to Copilot).
2. Open the PR it creates and click "View session".
3. Expand "Start MCP Servers". You should see the `pixellab` server start and list tools such as `create_character`, `animate_character`, etc.

## 5) Troubleshooting
- `printenv` in the agent shell won't show `COPILOT_MCP_PIXELLAB`. That is expected. Only the local MCP process receives `PIXELLAB_API_KEY` because of the `env` mapping.
- If startup fails, check:
  - The spec URL is reachable from the runner.
  - The header name matches PixelLab's requirement.
  - `.github/copilot-setup-steps.yml` ran and Node/npx are available.
