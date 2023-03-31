package com.peopleapp.dto;

import com.peopleapp.enums.MessageCodes;
import com.peopleapp.enums.SortingOrder;
import com.peopleapp.exception.BadRequestException;
import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class SortElement implements Comparable {

    private String dbKey;

    private int order;

    private int elementPriority;

    public SortElement(String dbKey, int order, int elementPriority) {
        this.dbKey = dbKey;
        this.order = order;
        this.elementPriority = elementPriority;
    }

    public Sort.Order getSortedOrder() {
        Sort.Order sortOrder = null;
        if (order == SortingOrder.DESCENDING_ORDER.getValue()) {
            sortOrder = new Sort.Order(Sort.Direction.DESC, dbKey);
        } else if (order == SortingOrder.ASCENDING_DEFAULT.getValue()
                || order == SortingOrder.ASCENDING_ORDER.getValue()) {
            sortOrder = new Sort.Order(Sort.Direction.ASC, dbKey);
        } else {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }

        return sortOrder;
    }

    @Override
    public int compareTo(Object object) {
        if (object instanceof SortElement) {
            SortElement sortElement = (SortElement) object;
            int comparatorValue;
            if (this.elementPriority < sortElement.elementPriority) {
                comparatorValue = -1;
            } else {
                comparatorValue = ((this.elementPriority == sortElement.elementPriority) ? 0 : 1);
            }

            return comparatorValue;
        }
        return 0;
    }
}
