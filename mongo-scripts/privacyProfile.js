db.systemPrivacyProfiles.remove({})

db.systemPrivacyProfiles.insert(
[
  {
    "_class": "com.peopleapp.model.SystemPrivacyProfile",
    "profileName": "Acquaintance",
    "profileKeyList": [
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "PHONENUMBER",
        "label": "PL.00.00"
      }
    ],
    "isDefault": true,
    "isPublic": false
  },
  {
    "_class": "com.peopleapp.model.SystemPrivacyProfile",
    "profileName": "Professional",
    "profileKeyList": [
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "PHONENUMBER",
        "label": "PL.00.02"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "EMAIL",
        "label": "PL.01.01"
      }
    ],
    "isDefault": false,
    "isPublic": false
  },
  {
    "_class": "com.peopleapp.model.SystemPrivacyProfile",
    "profileName": "Friends",
    "profileKeyList": [
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "PHONENUMBER",
        "label": "PL.00.00"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "EMAIL",
        "label": "PL.01.04"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "ADDRESS",
        "label": "PL.03.00"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "SOCIALPROFILE",
        "label": "PL.02.00"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "SOCIALPROFILE",
        "label": "PL.02.03"
      }
    ],
    "isDefault": false,
    "isPublic": false
  },
  {
    "_class": "com.peopleapp.model.SystemPrivacyProfile",
    "profileName": "Family",
    "profileKeyList": [
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "PHONENUMBER",
        "label": "PL.00.00"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "PHONENUMBER",
        "label": "PL.00.02"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "EMAIL",
        "label": "PL.01.01"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "EMAIL",
        "label": "PL.01.04"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "ADDRESS",
        "label": "PL.03.00"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "SOCIALPROFILE",
        "label": "PL.02.00"
      },
      {
        "_class": "com.peopleapp.model.ProfileKey",
        "category": "SOCIALPROFILE",
        "label": "PL.02.03"
      }
    ],
    "isDefault": false,
    "isPublic": false
  },
  {
    "_class": "com.peopleapp.model.SystemPrivacyProfile",
    "profileName": "Public",
    "isDefault": false,
    "isPublic": true
  }
]
);
