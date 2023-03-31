package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum NetworkCategoryType {

    SPORTS("Sports"),
    HEALTH_AND_FITNESS("Health & Fitness"),
    OUTDOOR_ACTIVITIES("Outdoor Activities"),
    GAMES("Games"),
    ENTERTAINMENT("Entertainment"),
    SPIRITUAL_AND_INSPIRATION("Spiritual & Inspiration"),
    TRAVEL("Travel"),
    CULTURE_AND_LIFESTYLE("Culture & Lifestyle"),
    HOME_AND_GARDEN("Home & Garden"),
    SUPPORT_AND_COMFORT("Support & Comfort"),
    COMMUNITY("Community"),
    FOOD("Food"),
    EDUCATION("Education"),
    SCIENCE_AND_TECHNOLOGY("Science & Technology"),
    PROFESSIONAL("Professional"),
    ANIMALS_AND_PETS("Animals & Pets"),
    HOBBY("Hobby");

    NetworkCategoryType(String value) {
        this.value = value;
    }

    private static final Map<String, NetworkCategoryType> CATEGORY_MAP = new HashMap<>();

    static {
        for (NetworkCategoryType myEnum : values()) {
            CATEGORY_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    public String getValue(){
        return value;
    }

    public static NetworkCategoryType getByValue(String value) {
        return CATEGORY_MAP.get(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
