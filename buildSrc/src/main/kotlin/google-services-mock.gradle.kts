import java.io.FileWriter

val googleServicesJson = file("google-services.json")
if (!googleServicesJson.exists()) {
    val writer = FileWriter(googleServicesJson)
    writer.write("""{
  "project_info": {
    "project_number": "1234567890",
    "project_id": "dummy-project",
    "storage_bucket": "dummy-project.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:1234567890:android:0000000000000000000000",
        "android_client_info": {
          "package_name": "net.dotevolve.benchmark"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "dummy-api-key"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    },
    {
      "client_info": {
        "mobilesdk_app_id": "1:1234567890:android:0000000000000000000001",
        "android_client_info": {
          "package_name": "net.dotevolve.benchmark.debug"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "dummy-api-key"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}""")
    writer.close()
}
