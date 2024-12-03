## Beaver IoT Integrations
Integrations are the primary means for Beaver IoT to interact with third-party services, devices, platforms, etc., enabling device connectivity, device control, and feature expansion.

## Start building your integrations

The following is the directory structure of this project.

```
  beaver-iot-integrations/
  ├── application-dev/
  │ ├── src/main/java/com/milesight/beaveriot/DevelopApplication.java # Start and debug your integrations from here.
  │ ├── integrations/                                                 # integration directory
  │ │ └── sample-integrations/                                        # Sample integrations
  │ │ └── ...                                                         # All integrations
```

If you want to develop your own integration, please create a new integration package under the `beaver-iot-integrations/integrations/` directory. For more information, please refer to [Quick Start](https://www.milesight.com/beaver-iot/docs/dev-guides/backend/build-integration) of Integration Development.
