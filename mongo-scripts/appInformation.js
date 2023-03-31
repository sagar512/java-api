db.appInformation.remove({})

db.appInformation.insert(
[
  {deviceTypeId : 1, buildVersion : "1.1.9", buildNumber : 9, forceUpdateRequired: false },
  {deviceTypeId : 2, buildVersion : "1.0", buildNumber : 30, forceUpdateRequired: false }
]
);
