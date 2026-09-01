package org.example.veportal.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "teaching_logs", uniqueConstraints = {
        @UniqueConstraint(name = "uq_teaching_logs_session", columnNames = "session_id")
})
public class TeachingLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private ClassSession session;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "remarks", columnDefinition = "text")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private UserAccount updatedByUser;

    @BatchSize(size = 25)
    @OneToMany(mappedBy = "log", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<TeachingLogConcept> concepts = new ArrayList<>();

    public ClassSession getSession() {
        return session;
    }

    public void setSession(ClassSession session) {
        this.session = session;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public UserAccount getUpdatedByUser() {
        return updatedByUser;
    }

    public void setUpdatedByUser(UserAccount updatedByUser) {
        this.updatedByUser = updatedByUser;
    }

    public List<TeachingLogConcept> getConcepts() {
        return concepts;
    }

    public void setConcepts(List<TeachingLogConcept> concepts) {
        this.concepts = concepts;
    }
}
