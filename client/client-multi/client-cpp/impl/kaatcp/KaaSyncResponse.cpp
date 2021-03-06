/*
 * Copyright 2014 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "kaa/kaatcp/KaaSyncResponse.hpp"
#include <arpa/inet.h>

namespace kaa {


KaaSyncResponse::KaaSyncResponse(const char * payload, std::uint32_t size) : isZipped_(false), isEncrypted_(false), messageId_(0)
{
    parseMessage(payload, size);
}

void KaaSyncResponse::parseMessage(const char * payload, std::uint32_t size)
{
    payload += 9;
    messageId_ = ntohs(*(std::uint16_t *)payload);
    payload += 2;
    isZipped_ = (*payload) & KaaTcpCommon::KAA_SYNC_ZIPPED_BIT;
    isEncrypted_ = (*payload) & KaaTcpCommon::KAA_SYNC_ENCRYPTED_BIT;
    ++payload;
    payload_.assign((const std::uint8_t*) payload, (const std::uint8_t*) payload + size - KaaTcpCommon::KAA_SYNC_HEADER_LENGTH);
}

}


