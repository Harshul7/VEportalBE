package org.example.veportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "group_topic_plans", uniqueConstraints = @UniqueConstraint(name = "uq_group_topic_plan", columnNames = {"group_id", "chapter_id"}))
public class GroupTopicPlan extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private CourseGroup group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;
    @Column(name = "week_number", nullable = false)
    private Integer weekNumber = 1;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 1;
    @Column(nullable = false)
    private boolean locked = false;
    public CourseGroup getGroup() { return group; }
    public void setGroup(CourseGroup group) { this.group = group; }
    public Chapter getChapter() { return chapter; }
    public void setChapter(Chapter chapter) { this.chapter = chapter; }
    public Integer getWeekNumber() { return weekNumber; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
}
