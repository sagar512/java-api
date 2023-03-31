db.systemTagsUser.remove({})

db.systemTagsUser.insert(
{
  "_class": "com.peopleapp.model.SystemTagUser",
  "tagList": [
    {
      "_class": "com.peopleapp.model.SystemTagData",
      "tag": "funny",
      "priority": 1
    },
    {
      "_class": "com.peopleapp.model.SystemTagData",
      "tag": "graphicdesigner",
      "priority": 2
    },
    {
      "_class": "com.peopleapp.model.SystemTagData",
      "tag": "musician",
      "priority": 3
    },
    {
      "_class": "com.peopleapp.model.SystemTagData",
      "tag": "gearhead",
      "priority": 4
    },
    {
      "_class": "com.peopleapp.model.SystemTagData",
      "tag": "bff",
      "priority": 5
    },
    {
      "_class": "com.peopleapp.model.SystemTagData",
      "tag": "gamer",
      "priority": 6
    },
    {
      "_class": "com.peopleapp.model.SystemTagData",
      "tag": "entrepreneur",
      "priority": 7
    },
    {
      "_class": "com.peopleapp.model.SystemTagData",
      "tag": "golfer",
      "priority": 8
    },
    {
      "_class": "com.peopleapp.model.SystemTagData",
      "tag": "foodie",
      "priority": 9
    },
    {
      "_class": "com.peopleapp.model.SystemTagData",
      "tag": "mentor",
      "priority": 10
    }
  ]
}
);
