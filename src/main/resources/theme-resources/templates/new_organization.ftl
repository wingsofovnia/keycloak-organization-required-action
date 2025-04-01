<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('phone_number'); section>
    <#if section = "header">
        ${msg("orgCreate")}
    <#elseif section = "form">
        <p>${helpText!msg("orgHelpText")}</p>
        <br/>

        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form id="kc-create-organization-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                    <div class="${properties.kcFormGroupClass!}">
                        <label for="orgName" class="${properties.kcLabelClass!}">
                            <strong>${msg("orgNameLabel")}</strong>
                        </label>

                        <input
                            tabindex="1"
                            id="orgName"
                            aria-invalid="<#if messagesPerField.existsError('orgName')>true</#if>"
                            class="${properties.kcInputClass!}"
                            name="orgName"
                            value="${orgName!}"
                            type="text"
                            autofocus
                            dir="ltr"
                        />

                        <#if messagesPerField.existsError('orgName')>
                            <span id="input-error-orgName" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('orgName'))?no_esc}
                            </span>
                        </#if>
                    </div>

                    <#if !isDomainGenerationEnabled>
                    <div class="${properties.kcFormGroupClass!}">
                        <label for="orgDomain" class="${properties.kcLabelClass!}">
                            <strong>${msg("orgDomainLabel")}</strong>
                        </label>

                        <input
                            type="text"
                            id="orgDomain"
                            name="orgDomain"
                            class="${properties.kcInputClass!}"
                            value="${orgDomain!}"
                            required
                            aria-invalid="<#if messagesPerField.existsError('orgDomain')>true</#if>"
                        />

                        <#if messagesPerField.existsError('orgDomain')>
                            <span id="input-error-orgDomain" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('orgDomain'))?no_esc}
                            </span>
                        </#if>
                    </div>
                    </#if>

                    <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                        <input tabindex="4"
                               class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="submit" id="kc-submit" type="submit" value="${msg("doSubmit")}"/>
                    </div>
                </form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
