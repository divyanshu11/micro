package com.proptiger.app.mvc.order;

import com.proptiger.core.model.BaseModel;

/**
*
* @author swapnil
*
*/
public class SellerCallMessage extends BaseModel{
	 /**
    *
    */
   private static final long serialVersionUID = 7963001395824898499L;

   private String     Type;
   private String     Message;

   public String getType() {
       return Type;
   }

   public void setType(String type) {
       Type = type;
   }

   public String getMessage() {
       return Message;
   }

   public void setMessage(String message) {
       Message = message;
   }
}
