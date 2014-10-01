/* ------------------------------------------------
Copyright 2014 AT&T Intellectual Property
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 ------------------------------------------- */

#ifndef __XML_PARSER_H
#define __XML_PARSER_H

#include <string>
#include <vector>

/////////////////////////
//	for resource parser

class xml_parser {
public:
	virtual void startElement(const char *name, std::vector<std::string> &attr_vec, std::vector<std::string> &val_vec) {};
	virtual void endElement(const char *name) {};
};

#endif	// __XML_PARSER_H