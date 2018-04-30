package com.wbiag.util;

import com.workbrain.app.ta.model.*;
import com.workbrain.util.NestedRuntimeException;
import com.workbrain.util.StringHelper;
import com.workbrain.util.TypedList;
import com.workbrain.util.NameValue;
import com.workbrain.util.DateHelper;
import com.workbrain.util.XMLHelper;
import java.util.*;
/**
 * List of NameValue classes, allows getting by name. Not to be used with long lists
 */
public class NameValueList extends TypedList{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(NameValueList.class);

    public NameValueList() {
        super(NameValue.class , false);
    }

    public void add(String name, String value) {
        add(new NameValue(name, value));
    }

    public NameValue getByName(String name){
        NameValue ret = null;
        Iterator iter = iterator();
        while (iter.hasNext()) {
            NameValue item = (NameValue)iter.next();
            if (name.equals(item.getName())) {
                ret = item;
            }
        }
        return ret;
    }


    public static List toListOfNameValueLists(WorkDetailList wdList, List fields) {
        List ret = new ArrayList();
        Iterator iterWd = wdList.iterator();
        while (iterWd.hasNext()) {
            WorkDetailData wd = (WorkDetailData) iterWd.next();

            NameValueList nvList = new NameValueList();

            Iterator iter = fields.iterator();
            while (iter.hasNext()) {
                String fld = (String) iter.next();
                Object val = null;
                try {
                    val = wd.getField(fld);
                }
                catch (Exception ex) {
                    throw new NestedRuntimeException("Error in resolving field name :" + fld + ". Check RuleTrace Config definition", ex);
                }
                nvList.add(fld, resolveVal(val));
            }
            ret.add(nvList) ;
        }
        return ret;
    }

    public static  NameValueList toNameValueList(RecordData rd , List fields) {
        NameValueList nvList = new NameValueList();
        Iterator iter = fields.iterator();
        while (iter.hasNext()) {
            String fld = (String) iter.next();
            nvList.add(new NameValue(fld, resolveVal(rd.getField(fld))));
        }
        return nvList;
    }

    private static final String DATE_FMT = "MM/dd/yyyy HH:mm";

    private static String resolveVal(Object val) {
        String ret = null;
        if (!StringHelper.isEmpty(val)) {
            if ((val instanceof java.util.Date) || (val instanceof java.sql.Date)) {
                ret = DateHelper.convertDateString( (java.util.Date)val,
                    DATE_FMT);
            }
            else {
                ret = XMLHelper.escapeXMLValue(val.toString());
            }
        }
        else {
            ret = "";
        }
        return ret;
    }

    public boolean equals(Object o) {
        boolean ret = true;
        if (o == null) {
            return false;
        }
        NameValueList nvl = (NameValueList)o;
        Iterator iter = nvl.iterator();
        while (iter.hasNext()) {
            NameValue item = (NameValue) iter.next();
            NameValue thisOne = getByName(item.getName());
            if (thisOne != null) {
                ret &= StringHelper.equals(item.getValue() , thisOne.getValue());
            }
            if (!ret) {
                break;
            }
        }
        return ret;
    }
}


