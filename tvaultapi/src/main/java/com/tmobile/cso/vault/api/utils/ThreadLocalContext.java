// =========================================================================
// Copyright 2019 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================

package com.tmobile.cso.vault.api.utils;

import java.util.HashMap;
import java.util.Map;

public class ThreadLocalContext {

	private static final ThreadLocal<Map<String, String>>  CURRENT = ThreadLocal.withInitial(HashMap::new);

    public static Map<String, String> getCurrentMap() {
        return CURRENT.get();
    }

    public static void setCurrentMap(Map<String, String> map) {
        CURRENT.set(map);
    }

	public void unloadCurrentMap() {
		CURRENT.remove();
	}
}
