package com.proptiger.app.model.transaction;

import java.util.HashMap;
import java.util.Map;

import com.proptiger.core.enums.SaleTypeEnum;
import com.proptiger.core.exception.BadRequestException;
//import com.proptiger.core.model.enums.transaction.MasterProductCategoryEnum;

public enum MasterProductCategoryEnum {

	Buy(1), Rent(2), Other(3);

    int                                                          id;
    private static final Map<Integer, MasterProductCategoryEnum> masterProductCategoryById = new HashMap<>();

    static {
        for (MasterProductCategoryEnum masterProductCategoryEnum : MasterProductCategoryEnum.values()) {
            masterProductCategoryById.put(masterProductCategoryEnum.getId(), masterProductCategoryEnum);
        }
    }

    private MasterProductCategoryEnum(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static MasterProductCategoryEnum getMasterProductCategoryEnumById(int id) {
        return masterProductCategoryById.get(id);
    }

    public static int getMasterProductCategoryEnumIdByName(String name) {
        int saleTypeId = -1;
        if (MasterProductCategoryEnum.Buy.name().equalsIgnoreCase(name))
            saleTypeId = MasterProductCategoryEnum.Buy.getId();
        else if (MasterProductCategoryEnum.Rent.name().equalsIgnoreCase(name)) {
            saleTypeId = MasterProductCategoryEnum.Rent.getId();
        }
        else if (MasterProductCategoryEnum.Other.name().equalsIgnoreCase(name)) {
            saleTypeId = MasterProductCategoryEnum.Other.getId();
        }
        else {
            throw new BadRequestException("Invalid product payment type");
        }
        return saleTypeId;
    }

    public static Integer getProductCategoryEnumIdByIcrmSaleTypeId(Integer saleTypeId) {
        int productCategoryId = MasterProductCategoryEnum.Other.id;
        switch (SaleTypeEnum.getSaleType(saleTypeId)) {
            case BUY:
            case PRIMARY:
            case RESALE:
                productCategoryId = MasterProductCategoryEnum.Buy.id;
                break;
            case RENT:
                productCategoryId = MasterProductCategoryEnum.Rent.id;
                break;
            default:
                break;
        }

        return productCategoryId;
    }
}
