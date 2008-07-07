/**
 * Copyright (c) 2000-2008 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet.expando.service.persistence;

/**
 * <a href="ExpandoRowFinderUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class ExpandoRowFinderUtil {
	public static int countByTC_TN(long classNameId, java.lang.String tableName)
		throws com.liferay.portal.SystemException {
		return getFinder().countByTC_TN(classNameId, tableName);
	}

	public static java.util.List<com.liferay.portlet.expando.model.ExpandoRow> findByTC_TN(
		long classNameId, java.lang.String tableName, int start, int end)
		throws com.liferay.portal.SystemException {
		return getFinder().findByTC_TN(classNameId, tableName, start, end);
	}

	public static com.liferay.portlet.expando.model.ExpandoRow fetchByTC_TN_C(
		long classNameId, java.lang.String tableName, long classPK)
		throws com.liferay.portal.SystemException {
		return getFinder().fetchByTC_TN_C(classNameId, tableName, classPK);
	}

	public static ExpandoRowFinder getFinder() {
		return _getUtil()._finder;
	}

	public void setFinder(ExpandoRowFinder finder) {
		_finder = finder;
	}

	private static ExpandoRowFinderUtil _getUtil() {
		if (_util == null) {
			_util = (ExpandoRowFinderUtil)com.liferay.portal.kernel.bean.PortalBeanLocatorUtil.locate(_UTIL);
		}

		return _util;
	}

	private static final String _UTIL = ExpandoRowFinderUtil.class.getName();
	private static ExpandoRowFinderUtil _util;
	private ExpandoRowFinder _finder;
}