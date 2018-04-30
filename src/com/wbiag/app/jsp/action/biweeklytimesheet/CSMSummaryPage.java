package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.util.*;

import com.workbrain.app.jsp.action.timesheet.SummaryPage;

public class CSMSummaryPage extends SummaryPage implements java.io.Serializable, Cloneable {
    private List _elapsedTimeLines = new ArrayList();
    private List _summaryTotals = new ArrayList();
    private int _summaryTotal;
    private List fieldNames = new ArrayList();

    public Object clone() throws CloneNotSupportedException {
        CSMSummaryPage sp = new CSMSummaryPage();

        for (int i = 0; i < _elapsedTimeLines.size(); i++) {
            sp._elapsedTimeLines.add(((CSMElapsedTimeLine)_elapsedTimeLines.get(i)).clone());
        }

        for (int i = 0; i < _summaryTotals.size(); i++) {
            sp._summaryTotals.add(new Integer(((Integer)_summaryTotals.get(i)).intValue()));
        }
        sp._summaryTotal = _summaryTotal;
        sp.fieldNames = (List)((ArrayList)fieldNames).clone();

        return sp;
    }

    public List getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames( List fieldNames ) {
        this.fieldNames = fieldNames;
    }

    public List getElapsedTimeLines() {
        return _elapsedTimeLines;
    }

    public void setElapsedTimeLines( List elapsedTimeLines ) {
        _elapsedTimeLines = elapsedTimeLines;
    }

    public List getSummaryTotals() {
        return _summaryTotals;
    }

    public void setSummaryTotals( List summaryTotals ) {
        _summaryTotals = summaryTotals;
    }

    public int getSummaryTotal() {
        return _summaryTotal;
    }

    public void setSummaryTotal( int summaryTotal ) {
        _summaryTotal = summaryTotal;
    }

    public String toString() {
        return "CSMSummaryPage : " +
                "elapsedTimeLines = " + _elapsedTimeLines +
                ", summaryTotals = " + _summaryTotals +
                ", summaryTotal = " + _summaryTotal;
    }
}

