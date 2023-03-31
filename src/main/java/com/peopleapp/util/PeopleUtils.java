package com.peopleapp.util;

import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.KeyValueData;
import com.peopleapp.dto.SortElement;
import com.peopleapp.enums.RequestType;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.data.domain.Sort;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;


public class PeopleUtils {

    private PeopleUtils() {

    }

    public static boolean isNullOrEmpty(final Collection<?> c) {
        return c == null || c.isEmpty();
    }

    public static boolean isNullOrEmpty(final Map<?, ?> m) {
        return m == null || m.isEmpty();
    }

    public static boolean isNullOrEmpty(String key) {
        return key == null || key.equals("");
    }

    public static <T> Iterable<T> emptyIfNull(Iterable<T> iterable) {
        return iterable == null ? Collections.<T>emptyList() : iterable;
    }

    public static ObjectId convertStringToObjectId(String parameter) {
        return new ObjectId(parameter);
    }

    public static List<ObjectId> convertStringToObjectId(List<String> parameterList) {
        List<ObjectId> newParamList = new ArrayList<>();
        for (String parameter : emptyIfNull(parameterList)) {
            newParamList.add(new ObjectId(parameter));
        }
        return newParamList;
    }

    public static String convertObjectIdToString(ObjectId paramter) {
        return paramter.toString();
    }

    public static Boolean compareValues(String str1, String str2) {
        return StringUtils.equalsIgnoreCase(str1, str2);
    }

    public static Boolean compareValues(Object obj1, Object obj2) {
        return ObjectUtils.equals(obj1, obj2);
    }

    public static <T> void setIfNotNullOrEmpty(final Consumer<T> setter, final T value) {
        if (value != null && !value.toString().isEmpty()) {
            setter.accept(value);
        }
    }


    public static DateTime getCurrentTimeInUTC() {
        return new DateTime(DateTimeZone.UTC);
    }

    public static DateTime getUpdatedTimeInUTC(DateTime givenTime, int timeInMinutes) {
        return givenTime.plusMinutes(timeInMinutes);
    }

    public static Boolean checkIfBeforeNow(DateTime givenTime) {
        return givenTime.isBeforeNow();
    }

    public static List<String> getRequestTypeList() {

        List<String> requestTypeList = new ArrayList<>();
        requestTypeList.add(RequestType.CONNECTION_REQUEST.getValue());
        requestTypeList.add(RequestType.INTRODUCTION_REQUEST.getValue());
        requestTypeList.add(RequestType.NETWORK_MEMBER_INVITE.getValue());
        requestTypeList.add(RequestType.NETWORK_JOIN_REQUEST.getValue());
        requestTypeList.add(RequestType.MORE_INFO_REQUEST.getValue());

        return requestTypeList;
    }

    // Function to remove duplicates from an ArrayList
    public static <T> List<T> removeDuplicates(List<T> list) {
        Set<T> set = new LinkedHashSet<>();
        set.addAll(list);
        list.clear();
        list.addAll(set);
        return list;
    }

    public static Sort getSort(Set<SortElement> sorted) {

        Sort sort = null;
        Iterator<SortElement> iterator = sorted.iterator();
        List<Sort.Order> sortOrder = new ArrayList<>();

        //iterating over each sortElement and fetching SortOrder of element
        while (iterator.hasNext()) {
            sortOrder.add(iterator.next().getSortedOrder());
        }

        //passing list of Sort.Order and generating Sort object
        if (!sortOrder.isEmpty()) {
            sort = Sort.by(sortOrder);
        }

        return sort;
    }
    public static String getMobileNumberWithCoutryCode(ContactNumberDTO mobileNumber) {
		String contactNumber = "";
		if (!PeopleUtils.isNullOrEmpty(mobileNumber.getCountryCode())
				&& !PeopleUtils.isNullOrEmpty(mobileNumber.getPhoneNumber())) {
			contactNumber = mobileNumber.getCountryCode().concat(mobileNumber.getPhoneNumber());
		}
		return contactNumber;
	}

    public static String getDefaultOrEmpty(String string) {
    	return  (Optional.ofNullable(string).orElse(""));
    }
    
    public static String convertDate(String date) {
        try {
             SimpleDateFormat simpleDateFormat= new SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH);
             Date TempDate= simpleDateFormat.parse(date);
             SimpleDateFormat serverFormat= new SimpleDateFormat("EEEE d MMM, yyyy",Locale.ENGLISH);
             return serverFormat.format(TempDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    
	public static String convertMobileListToString(List<KeyValueData> list) {
		StringBuilder str = new StringBuilder();
		for(KeyValueData data : PeopleUtils.emptyIfNull(list)) {
			if(data.getKey().equalsIgnoreCase("countryCode")) {
				str.append(data.getVal());
			}else {
				str.append(data.getVal().replaceFirst("(\\d{3})(\\d{3})(\\d+)", "($1)$2-$3"));
			}
		}
		return str.toString();
	}
	
	public static String convertOtherKeyValueToString(List<KeyValueData> list) {
		StringBuilder str = new StringBuilder();
		for(KeyValueData data : PeopleUtils.emptyIfNull(list)) {
			str.append(data.getVal());
		}
		return str.toString();
	}
	
	public static String convertAddressListToString(List<KeyValueData> list) {
		StringBuilder str = new StringBuilder();
		for(KeyValueData data : PeopleUtils.emptyIfNull(list)) {
			if(!data.getKey().equalsIgnoreCase("formattedAddress")) {
				if(data.getKey().equalsIgnoreCase("postcode")) {
					str.append(data.getVal());
				}else {
					str.append(data.getVal()+", ");	
				}
			}
		}
		return str.toString();
	}
}
