<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ include file="/init.jsp" %>

<%
SelectSegmentsEntryOrganizationsDisplayContext selectSegmentsEntryOrganizationsDisplayContext = (SelectSegmentsEntryOrganizationsDisplayContext)request.getAttribute(SegmentsWebKeys.SELECT_SEGMENTS_ENTRY_ORGANIZATIONS_DISPLAY_CONTEXT);
%>

<clay:management-toolbar
	clearResultsURL="<%= selectSegmentsEntryOrganizationsDisplayContext.getClearResultsURL() %>"
	componentId="segmentsEntryOrganizationsManagementToolbar"
	disabled="<%= selectSegmentsEntryOrganizationsDisplayContext.isDisabledManagementBar() %>"
	filterDropdownItems="<%= selectSegmentsEntryOrganizationsDisplayContext.getFilterDropdownItems() %>"
	itemsTotal="<%= selectSegmentsEntryOrganizationsDisplayContext.getTotalItems() %>"
	searchActionURL="<%= selectSegmentsEntryOrganizationsDisplayContext.getSearchActionURL() %>"
	searchContainerId="segmentsEntryOrganizations"
	searchFormName="searchFm"
	showSearch="<%= selectSegmentsEntryOrganizationsDisplayContext.isShowSearch() %>"
	sortingOrder="<%= selectSegmentsEntryOrganizationsDisplayContext.getOrderByType() %>"
	sortingURL="<%= selectSegmentsEntryOrganizationsDisplayContext.getSortingURL() %>"
	viewTypeItems="<%= selectSegmentsEntryOrganizationsDisplayContext.getViewTypeItems() %>"
/>

<aui:form cssClass="container-fluid-1280" name="fm">
	<liferay-ui:search-container
		id="segmentsEntryOrganizations"
		searchContainer="<%= selectSegmentsEntryOrganizationsDisplayContext.getOrganizationSearchContainer() %>"
	>
		<liferay-ui:search-container-row
			className="com.liferay.portal.kernel.model.Organization"
			escapedModel="<%= true %>"
			keyProperty="organizationId"
			modelVar="organization"
		>
			<liferay-ui:search-container-column-text
				name="name"
				orderable="<%= true %>"
				property="name"
			/>

			<liferay-ui:search-container-column-text
				name="parent-organization"
				value="<%= HtmlUtil.escape(organization.getParentOrganizationName()) %>"
			/>

			<liferay-ui:search-container-column-text
				name="type"
				orderable="<%= true %>"
				value="<%= LanguageUtil.get(request, organization.getType()) %>"
			/>
		</liferay-ui:search-container-row>

		<liferay-ui:search-iterator
			displayStyle="<%= selectSegmentsEntryOrganizationsDisplayContext.getDisplayStyle() %>"
			markupView="lexicon"
		/>
	</liferay-ui:search-container>
</aui:form>

<aui:script use="liferay-search-container">
	var searchContainer = Liferay.SearchContainer.get('<portlet:namespace />segmentsEntryOrganizations');

	searchContainer.on(
		'rowToggled',
		function(event) {
			var result = {};

			var data = event.elements.allSelectedElements.getDOMNodes();

			if (data.length) {
				result = {
					data: data
				};
			}

			Liferay.Util.getOpener().Liferay.fire(
				'<%= HtmlUtil.escapeJS(selectSegmentsEntryOrganizationsDisplayContext.getEventName()) %>',
				result);
		}
	);
</aui:script>