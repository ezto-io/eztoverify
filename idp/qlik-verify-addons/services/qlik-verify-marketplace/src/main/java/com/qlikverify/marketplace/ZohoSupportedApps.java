package com.qlikverify.marketplace;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public enum ZohoSupportedApps {


    ZOHO_PAYROLL("Zoho Payroll", "https://payroll.zoho.com", "payroll_logo.svg"),
    ZOHO_CRM("Zoho CRM", "https://crm.zoho.com",  "crm_logo.svg"),
    ZOHO_CRM_PLUS("Zoho CRM Plus", "https://crmplus.zoho.com", "crmplus_logo.svg"),
    ZOHO_MARKETING_PLUS("Zoho Marketing Plus", "https://marketingplus.zoho.com", "marketingplus_logo.svg"),
    ZOHO_BIGIN("Zoho Bigin", "https://bigin.zoho.com", "bigin_logo.svg"),
    ZOHO_FORMS("Zoho Forms", "https://forms.zoho.com", "forms_logo.svg"),
    ZOHO_SIGN("Zoho Sign", "https://sign.zoho.com", "sign_logo.svg"),
    ZOHO_SOCIAL("Zoho Social", "https://social.zoho.com", "social_logo.svg"),
    ZOHO_SALESIQ("Zoho SalesIQ", "https://salesiq.zoho.com", "salesiq_logo.svg"),
    ZOHO_SURVEY("Zoho Survey", "https://survey.zoho.com", "survey_logo.svg"),
    ZOHO_CAMPAIGNS("Zoho Campaigns", "https://campaigns.zoho.com", "campaigns_logo.svg"),
    ZOHO_SITES("Zoho Sites", "https://sites.zoho.com", "sites_logo.svg"),
    ZOHO_CONTACTMANAGER("Zoho ContactManager", "https://contactmanager.zoho.com", "contactmanager_logo.svg"),
    ZOHO_PAGESENSE("Zoho PageSense", "https://pagesense.zoho.com", "pagesense_logo.svg"),
    ZOHO_BACKSTAGE("Zoho Backstage", "https://backstage.zoho.com", "backstage_logo.svg"),
    ZOHO_BOOKINGS("Zoho Bookings", "https://bookings.zoho.com", "bookings_logo.svg"),
    ZOHO_WEBINAR("Zoho Webinar", "https://webinar.zoho.com", "webinar_logo.svg"),
    ZOHO_BOOKS("Zoho Books", "https://books.zoho.com", "books_logo.svg"),
    ZOHO_INVENTORY("Zoho Inventory", "https://inventory.zoho.com", "inventory_logo.svg"),
    ZOHO_INVOICE("Zoho Invoice", "https://invoice.zoho.com", "invoice_logo.svg"),
    ZOHO_SUBSCRIPTIONS("Zoho Subscriptions", "https://subscriptions.zoho.com", "subscriptions_logo.svg"),
    ZOHO_EXPENSE("Zoho Expense", "https://expense.zoho.com", "expense_logo.svg"),
    ZOHO_MAIL("Zoho Mail", "https://mail.zoho.com", "mail_logo.svg"),
    ZOHO_CLIQ("Zoho Cliq", "https://cliq.zoho.com", "cliq_logo.svg"),
    ZOHO_CALENDAR("Zoho Calendar", "https://calendar.zoho.com", "calendar_logo.svg"),
    ZOHO_WORKDRIVE("Zoho WorkDrive", "https://workdrive.zoho.com", "workdrive_logo.svg"),
    ZOHO_WRITER("Zoho Writer", "https://writer.zoho.com", "writer_logo.svg"),
    ZOHO_SHEET("Zoho Sheet", "https://sheet.zoho.com", "sheet_logo.svg"),
    ZOHO_SHOW("Zoho Show", "https://show.zoho.com", "show_logo.svg"),
    ZOHO_PROJECTS("Zoho Projects", "https://projects.zoho.com", "projects_logo.svg"),
    ZOHO_SPRINTS("Zoho Sprints", "https://sprints.zoho.com", "sprints_logo.svg"),
    ZOHO_BUGTRACKER("Zoho BugTracker", "https://bugtracker.zoho.com", "bugtracker_logo.svg"),
    ZOHO_CONNECT("Zoho Connect", "https://connect.zoho.com", "connect_logo.svg"),
    ZOHO_MEETING("Zoho Meeting", "https://meeting.zoho.com", "meeting_logo.svg"),
    ZOHO_VAULT("Zoho Vault", "https://vault.zoho.com", "vault_logo.svg"),
    ZOHO_NOTEBOOK("Zoho Notebook", "https://notebook.zoho.com", "notebook_logo.svg"),
    ZOHO_TEAMINBOX("Zoho TeamInbox", "https://teaminbox.zoho.com", "teaminbox_logo.svg"),
    ZOHO_ZEPTOMAIL("Zoho ZeptoMail", "https://zeptomail.zoho.com", "zeptomail_logo.svg"),
    ZOHO_LEARN("Zoho Learn", "https://learn.zoho.com", "learn_logo.svg"),
    ZOHO_VOICE("Zoho Voice", "https://voice.zoho.com", "voice_logo.svg"),
    ZOHO_WIKI("Zoho Wiki", "https://wiki.zoho.com", "wiki_logo.svg"),
    ZOHO_DESK("Zoho Desk", "https://desk.zoho.com", "desk_logo.svg"),
    ZOHO_ASSIST("Zoho Assist", "https://assist.zoho.com", "assist_logo.svg"),
    ZOHO_SITE_24X7("Zoho Site24x7", "https://www.site24x7.com/login/success.jsp", "site24x7_logo.svg"),
    ZOHO_LENS("Zoho Lens", "https://lens.zoho.com", "lens_logo.svg"),
    ZOHO_RECRUIT("Zoho Recruit", "https://recruit.zoho.com", "recruit_logo.svg"),
    ZOHO_PEOPLE("Zoho People", "https://people.zoho.com", "people_logo.svg"),
    ZOHO_WORKERLY("Zoho Workerly", "https://workerly.zoho.com", "workerly_logo.svg"),
    ZOHO_CREATOR("Zoho Creator", "https://creator.zoho.com", "creator_logo.svg"),
    ZOHO_ANALYTICS("Zoho Analytics", "https://analytics.zoho.com", "analytics_logo.svg"),
    ZOHO_FLOW("Zoho Flow", "https://flow.zoho.com", "flow_logo.svg");

    private String name;
    private String relayStateLink;
    private String logoUrl;

    ZohoSupportedApps(String name, String relayStateLink, String logoUrl) {
        this.name = name;
        this.relayStateLink = relayStateLink;
        this.logoUrl = logoUrl;
    }

    public String getName(){
        return this.name;
    }
    public String getRelayStateLink(){
        return this.relayStateLink;
    }
    public String getLogoUrl(){
        return this.logoUrl;
    }

    public static List<String> getAllNames(){
        return Arrays.stream(ZohoSupportedApps.values()).map(x -> x.getName()).collect(Collectors.toList());
    }

    public static List<ZohoSupportedApps> getUrlByName(String name) {
        return Arrays.stream(ZohoSupportedApps.values())
                .filter(apps -> apps.name.equals(name)).collect(Collectors.toList());
    }
}
