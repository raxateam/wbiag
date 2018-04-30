package com.wbiag.app.ta.model;

import com.workbrain.app.ta.model.*;

public class EmployeeReaderData extends RecordData{

    private int emprdrgrpId;
    private int empId;
    private int rdrgrpId;
    
    
    public RecordData newInstance() {
        return new EmployeeReaderData();
    }

    public int getEmprdrgrpId(){
        return emprdrgrpId;
    }

    public void setEmprdrgrpId(int v){
        emprdrgrpId=v;
    }

    public int getEmpId(){
        return empId;
    }

    public void setEmpId(int v){
        empId=v;
    }
    
    public int getRdrgrpId(){
        return rdrgrpId;
    }
    public void setRdrgrpId(int v){
        rdrgrpId=v;
    }
    
    
    public String toString(){
          return
            "emprdrgrpId=" + emprdrgrpId + "\n" +
            "empId=" + empId + "\n" +
            "rdrgrpId=" + rdrgrpId + "\n" ;
    }
}
