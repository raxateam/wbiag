package com.wbiag.app.modules.reports.hcprintschedule;

import junit.framework.TestSuite;

import java.util.*;

import com.workbrain.app.ta.ruleengine.RuleTestCase;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

public class ContainerClassesTest extends RuleTestCase {

	private DBConnection conn = this.getConnection();

	public ContainerClassesTest(String testName) throws Exception {
        super(testName);
    }

	public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ContainerClassesTest.class);
        return result;
    }

	public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

	public void testTeam(){
        Date startDate = new Date();
        Date endDate = DateHelper.addDays(new Date(), 2);
        Team team = new Team(10003, "VUH 10N", startDate, endDate);
        Daypart daypart = new Daypart(10041, "12HR DAY", startDate, endDate);
        team.addDaypart(daypart);

        assertEquals(team.name, "VUH 10N");
        assertTrue(team.startDate.equals(startDate));
        assertTrue(team.endDate.equals(endDate));
        assertEquals(team.id, 10003);

        assertTrue(team.dayparts.size() == 1);
        assertTrue(team.dayparts.get(0) == daypart);
	}

    public void testDaypart(){
        Date startDate = new Date();
        Date endDate = DateHelper.addMinutes(new Date(), 60);
        Daypart daypart = new Daypart(10041, "12HR NGT", startDate, endDate);
        Job job = new Job(10041, "RN");
        daypart.addJob(job);

        assertEquals(daypart.id, 10041);
        assertEquals(daypart.name, "12HR NGT");
        assertTrue(daypart.startTime.equals(startDate));
        assertTrue(daypart.endTime.equals(endDate));
        assertEquals(daypart.jobs.size(), 1);
        assertEquals(daypart.jobs.get(0), job);
    }

    public void testJob(){

        Job job = new Job(10001, "RN");
        Employee emp = new Employee("Ajellu, Ali", "2.3");
        job.addEmployee(emp);

        assertEquals(job.id, 10001);
        assertEquals(job.name, "RN");
        assertEquals(job.employees.size(), 1);
        assertEquals(job.employees.get(0), emp);
    }

    public void testEmployee(){

        Date startDate = new Date();
        Date endDate   = DateHelper.addDays(new Date(),1);

        Employee emp = new Employee("Ajellu, Ali", "2.3");
        Schedule sched1 = new Schedule(startDate, "Ajellu, Ali", "RN", "9:00", "10:00", 60, "WRK", "VUH 10N", "VUH 10");
        Schedule sched2 = new Schedule(startDate, "Ajellu, Ali", "RN","10:00", "11:00", 60, "WRK", "VUH 10", "VUH 10");
        Schedule sched3 = new Schedule(endDate  , "Ajellu, Ali", "RN","10:00", "11:00", 60, "VAC", "VUH 10", "VUH 10");

        emp.addSchedule(sched1);
        emp.addSchedule(sched2);
        emp.addSchedule(sched3);

        assertEquals(emp.name, "Ajellu, Ali");
        assertEquals(emp.fteValue, "2.3");
        assertEquals(emp.schedules.size(), 2);

        assertEquals(emp.getSchedules(startDate).get(0), sched1);
        assertEquals(emp.getSchedules(startDate).get(1), sched2);
        assertEquals(emp.getSchedules(endDate).get(0), sched3);

    }

    public void testSchedule(){
        Date workDate = new Date();
        Schedule sched = new Schedule(workDate, "Ajellu, Ali", "RN",
                                "9:00", "10:00", 60, "WRK", "VUH 10N", "VUH 10");
        assertEquals(sched.workDate, workDate);
        assertEquals(sched.empName, "Ajellu, Ali");
        assertEquals(sched.job, "RN");
        assertEquals(sched.startTime, "9:00");
        assertEquals(sched.endTime, "10:00");
        assertEquals(sched.mins, 60);
        assertEquals(sched.timecode, "WRK");
        assertEquals(sched.team, "VUH 10N");
        assertEquals(sched.homeTeam, "VUH 10");


    }
}