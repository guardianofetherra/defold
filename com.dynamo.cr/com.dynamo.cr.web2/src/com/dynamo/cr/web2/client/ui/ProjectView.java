package com.dynamo.cr.web2.client.ui;

import java.util.Date;

import com.dynamo.cr.web2.client.CommitDesc;
import com.dynamo.cr.web2.client.Log;
import com.dynamo.cr.web2.client.ProjectInfo;
import com.dynamo.cr.web2.client.UserInfo;
import com.dynamo.cr.web2.client.UserInfoList;
import com.dynamo.cr.web2.shared.ClientUtil;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.CalendarUtil;

public class ProjectView extends Composite {
    public interface Presenter {
        void addMember(String email);
        void removeMember(int id);
        void deleteProject(ProjectInfo projectInfo);
    }

    private static ProjectDetailsUiBinder uiBinder = GWT
            .create(ProjectDetailsUiBinder.class);
    @UiField SideBar sideBar;
    @UiField SpanElement projectName;
    @UiField Button addMember;
    @UiField Button deleteProject;
    @UiField SpanElement description;
    /*
     * NOTE: Watch out for this bug:
     * http://code.google.com/p/google-web-toolkit/issues/detail?id=3533 We
     * add handler to the textbox
     */
    @UiField(provided = true) SuggestBox suggestBox;
    @UiField FlexTable members2;
    @UiField FlexTable commits2;
    @UiField HTMLPanel addMemberPanel;
    @UiField Anchor iOSDownload;
    @UiField Label signedExeInfo;

    private final MultiWordSuggestOracle suggestions = new MultiWordSuggestOracle();
    private Presenter listener;
    private ProjectInfo projectInfo;

    interface ProjectDetailsUiBinder extends UiBinder<Widget, ProjectView> {
    }

    public ProjectView() {
        suggestBox = new SuggestBox(suggestions);
        suggestBox.getElement().setPropertyString("placeholder", "enter email to add user");
        initWidget(uiBinder.createAndBindUi(this));

        Element element = members2.getElement();
        element.addClassName("table");
        element.addClassName("table-members");

        element = commits2.getElement();
        element.addClassName("table");
        element.addClassName("table-striped");
        element.addClassName("table-commits");

        addMember.addStyleName("btn btn-success");
        deleteProject.addStyleName("btn btn-danger");
    }

    public void clear() {
        this.projectName.setInnerText("");
        this.deleteProject.setVisible(false);
        this.description.setInnerText("");
        this.members2.removeAllRows();
        this.commits2.removeAllRows();
        this.addMemberPanel.setVisible(false);
        this.iOSDownload.setVisible(false);
        this.signedExeInfo.setText("");
    }

    public void setProjectInfo(int userId, ProjectInfo projectInfo, String iOSUrl) {
        this.projectInfo = projectInfo;
        this.projectName.setInnerText(projectInfo.getName());
        String description = projectInfo.getDescription();
        if (description.isEmpty()) {
            description = "(no description)";
        }
        this.description.setInnerText(description);

        boolean isOwner = userId == projectInfo.getOwner().getId();
        this.deleteProject.setVisible(isOwner);
        this.addMemberPanel.setVisible(isOwner);

        this.members2.removeAllRows();
        JsArray<UserInfo> membersList = projectInfo.getMembers();
        for (int i = 0; i < membersList.length(); ++i) {
            final UserInfo memberInfo = membersList.get(i);

            Image gravatar = new Image(ClientUtil.createGravatarUrl(memberInfo.getEmail(), 48));
            this.members2.setWidget(i, 0, gravatar);
            this.members2.setText(i, 1, memberInfo.getFirstName() + " " + memberInfo.getLastName());

            if (isOwner && !(memberInfo.getId() == userId)) {
                Button removeButton = new Button("Remove");
                removeButton.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        listener.removeMember(memberInfo.getId());
                    }
                });
                removeButton.addStyleName("btn btn-danger");
                this.members2.setWidget(i, 2, removeButton);
            } else {
                this.members2.setText(i, 2, " ");
            }

            CellFormatter cellFormatter = this.members2.getCellFormatter();
            cellFormatter.addStyleName(i, 0, "table-members-col0");
            cellFormatter.addStyleName(i, 1, "table-members-col1");
        }

        iOSDownload.setVisible(iOSUrl != null);
        if (iOSUrl != null) {
            signedExeInfo.setText("Download the executable on your iOS device for direct installation.");
        } else {
            signedExeInfo.setText("No signed executable found. Use Defold Editor to sign and upload for all project members.");
        }

        iOSDownload.setHref("itms-services://?action=download-manifest&url=" + iOSUrl);
    }

    public void setLog(int userId, Log log) {
        this.commits2.clear();
        JsArray<CommitDesc> commits = log.getCommits();
        DateTimeFormat sourceDF = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss Z");
        Date now = new Date();
        for (int i = 0; i < commits.length(); ++i) {
            final CommitDesc commit = commits.get(i);
            Date commitDate = sourceDF.parse(commit.getDate());
            String date = formatDate(now, commitDate);

            String msg = commit.getName() + " at " + date + "<br/>";
            msg += commit.getMessage().replace("\n", "<br/>");

            Image gravatar = new Image(ClientUtil.createGravatarUrl(commit.getEmail(), 32));
            this.commits2.setWidget(i, 0, gravatar);
            this.commits2.setHTML(i, 1, msg);
            CellFormatter cellFormatter = this.commits2.getCellFormatter();
            cellFormatter.addStyleName(i, 0, "table-commits-col0");
            cellFormatter.addStyleName(i, 1, "table-commits-col1");
        }
    }

    private String formatDate(Date now, Date before) {
        StringBuffer format = new StringBuffer("HH:mm");
        int days = CalendarUtil.getDaysBetween(before, now);
        if (days > 0) {
            format.append(", MMM d");
        }
        if (days >= 365) {
            format.append(", yyyy");
        }
        return DateTimeFormat.getFormat(format.toString()).format(before);
    }

    public void setPresenter(Presenter listener) {
        this.listener = listener;
        sideBar.setActivePage("projects");
    }

    public void setConnections(UserInfoList userInfoList) {
        suggestions.clear();
        JsArray<UserInfo> users = userInfoList.getUsers();
        for (int i = 0; i < users.length(); ++i) {
            suggestions.add(users.get(i).getEmail());
        }
    }

    @UiHandler("deleteProject")
    void onDeleteProjectClick(ClickEvent event) {
        if (Window.confirm("Are you sure you want to delete the project?")) {
            listener.deleteProject(this.projectInfo);
        }
    }

    @UiHandler("addMember")
    void onAddMemberClick(ClickEvent event) {
        String email = suggestBox.getText();
        System.out.println("!!");
        System.out.println(email);
        if (email != null && email.length() > 0) {
            listener.addMember(email);
            suggestBox.setText("");
        }
    }

    public void setUserInfo(String firstName, String lastName, String email) {
        sideBar.setUserInfo(firstName, lastName, email);
    }

}
