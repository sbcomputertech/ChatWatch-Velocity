# ChatWatch Velocity

> See [sbcomputertech/ChatWatch](https://github.com/sbcomputertech/ChatWatch) for the core server software.

### What is this?
This is a plugin for velocity proxy servers that captures chat and private messages and sends them to a ChatWatch instance.

### How to install
Download the latest release's jar file and add it to the `<velocity server>/plugins/` directory.
If there is no config file present, the plugin will create one by default. It looks as follows.

```json
{
  "serverAddress": "127.0.0.1",
  "serverPort": 8080,
  "ingestToken": "00000000-0000-0000-0000-000000000000",
  "messageCommands": [
    "msg",
    "tell",
    "w"
  ]
}
```

- `messageCommands` denotes commands used to send private messages, and should be commands used in the format `/command targetplayer message`
- `ingestToken` is a G/UUID that is used like an API key to submit messages to the server. **This must match the token specified in the server config!**