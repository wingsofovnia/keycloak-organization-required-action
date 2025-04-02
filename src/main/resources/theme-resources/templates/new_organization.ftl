<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('phone_number'); section>
    <#if section = "header">
        ${msg("orgCreate")}
    <#elseif section = "form">
        <p>${msg("orgHelpText")}</p>
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
                            value="${(formData.orgName)!''}"
                            type="text"
                            minLength="2"
                            required
                            autofocus
                            dir="ltr"
                        />

                        <#if messagesPerField.existsError('orgName')>
                            <span id="input-error-orgName" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('orgName'))?no_esc}
                            </span>
                        </#if>
                    </div>

                    <#list attributes?keys as attributeName>
                        <div class="${properties.kcFormGroupClass!}">
                            <label for="orgAttr_${attributeName}" class="${properties.kcLabelClass!}">
                                <strong>${msg(attributeName)}</strong>
                            </label>

                            <input
                                    tabindex="1"
                                    id="orgAttr_${attributeName}"
                                    aria-invalid="<#if messagesPerField.existsError("orgAttr_${attributeName}")>true</#if>"
                                    class="${properties.kcInputClass!}"
                                    name="orgAttr_${attributeName}"
                                    value="${(formData["orgAttr_${attributeName}"])!''}"
                                    type="${attributes[attributeName].type}"
                                    min="${attributes[attributeName].min}"
                                    max="${attributes[attributeName].max}"
                                    minLength="${attributes[attributeName].minLength}"
                                    maxLength="${attributes[attributeName].maxLength}"
                                    <#if attributes[attributeName].required?has_content>required</#if>
                                    autofocus
                                    dir="ltr"
                            />

                            <#if messagesPerField.existsError("orgAttr_${attributeName}")>
                                <span id="input-error-orgAttr_${attributeName}" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get("orgAttr_${attributeName}"))?no_esc}
                            </span>
                            </#if>
                        </div>
                    </#list>

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
                            value="${(formData.orgDomain)!''}"
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
