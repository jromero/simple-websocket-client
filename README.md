## Simple WebSocket Client (SWSC)

As the name states a very simple WebSocket client...

### Usage

`java -jar swsc-0.1.jar --help`

```
A simple WebSocket client.

Usage:
  swsc-0.1.jar [(--headern=<hn> --headerv=<hv>)...] <uri>

Example:
  swsc-0.1.jar --headern="Cookie" --headerv="USER=X" ws://echo.websocket.org

Options:
  --headern=<hn>  Header Name (note: Header Value must be present)
  --headerv=<hv>  Header Value (note: Header Name must be present)
```