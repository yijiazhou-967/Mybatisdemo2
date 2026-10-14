package com.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 项目可包含多名员工，同一员工也可以参与多个项目。 */
public class Project {
    private Integer projectId;
    private String projectCode;
    private String title;
    private LocalDate startDate;
    private Integer deleted;
    private List<Emp> emps = new ArrayList<>();

    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public List<Emp> getEmps() { return emps; }
    public void setEmps(List<Emp> emps) { this.emps = emps; }
}
