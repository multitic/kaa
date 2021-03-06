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

#ifndef LOG_HPP_
#define LOG_HPP_

#include "kaa/KaaDefaults.hpp"

#if KAA_LOG_LEVEL > KAA_LOG_LEVEL_NONE
#include "kaa/logging/LoggerFactory.hpp"

#include <string.h>
#include <boost/format.hpp>


#ifdef _WIN32
#define PATH_SEPARATOR '\\'
#else
#define PATH_SEPARATOR '/'
#endif

#define __LOGFILE (strrchr(__FILE__, PATH_SEPARATOR) ? strrchr(__FILE__, PATH_SEPARATOR) + 1 : __FILE__)

#endif

namespace kaa {

#if KAA_LOG_LEVEL > KAA_LOG_LEVEL_NONE
void kaa_log_message(const ILogger & logger, LogLevel level, const char *message, const char *file, size_t lineno);
void kaa_log_message(const ILogger & logger, LogLevel level, const std::string &message, const char *file, size_t lineno);
void kaa_log_message(const ILogger & logger, LogLevel level, const boost::format& message, const char *file, size_t lineno);

#endif

#if KAA_LOG_LEVEL >= KAA_LOG_LEVEL_FINE_TRACE
    #define KAA_LOG_FTRACE(message) kaa_log_message(LoggerFactory::getLogger(), LogLevel::TRACE,      (message), __LOGFILE, __LINE__);
#else
    #define KAA_LOG_FTRACE(message)
#endif
#if KAA_LOG_LEVEL >= KAA_LOG_LEVEL_TRACE
    #define KAA_LOG_TRACE(message)  kaa_log_message(LoggerFactory::getLogger(), LogLevel::TRACE,      (message), __LOGFILE, __LINE__);
#else
    #define KAA_LOG_TRACE(message)
#endif
#if KAA_LOG_LEVEL >= KAA_LOG_LEVEL_DEBUG
    #define KAA_LOG_DEBUG(message)  kaa_log_message(LoggerFactory::getLogger(), LogLevel::DEBUG,      (message), __LOGFILE, __LINE__);
#else
    #define KAA_LOG_DEBUG(message)
#endif
#if KAA_LOG_LEVEL >= KAA_LOG_LEVEL_INFO
    #define KAA_LOG_INFO(message)   kaa_log_message(LoggerFactory::getLogger(), LogLevel::INFO,       (message), __LOGFILE, __LINE__);
#else
    #define KAA_LOG_INFO(message)
#endif
#if KAA_LOG_LEVEL >= KAA_LOG_LEVEL_WARNING
    #define KAA_LOG_WARN(message)   kaa_log_message(LoggerFactory::getLogger(), LogLevel::WARNING,    (message), __LOGFILE, __LINE__);
#else
    #define KAA_LOG_WARN(message)
#endif
#if KAA_LOG_LEVEL >= KAA_LOG_LEVEL_ERROR
    #define KAA_LOG_ERROR(message)  kaa_log_message(LoggerFactory::getLogger(), LogLevel::ERROR,      (message), __LOGFILE, __LINE__);
#else
    #define KAA_LOG_ERROR(message)
#endif
#if KAA_LOG_LEVEL >= KAA_LOG_LEVEL_FATAL
    #define KAA_LOG_FATAL(message)  kaa_log_message(LoggerFactory::getLogger(), LogLevel::FATAL,      (message), __LOGFILE, __LINE__);
#else
    #define KAA_LOG_FATAL(message)
#endif

#if defined(KAA_THREADSAFE) && defined(KAA_MUTEX_LOGGING_ENABLED) && KAA_LOG_LEVEL > 4
template <typename LockType, typename MutableObject>
class MutexScopedLockLogger {
public:
    MutexScopedLockLogger(const char *name, MutableObject& m, const char *file, size_t line)
        : name_(name)
        , file_(file)
        , line_(line)
        , lock_(m, boost::defer_lock_t()) {
        kaa_log_message(LoggerFactory::getLogger(), LogLevel::DEBUG, (boost::format("Locking %1% mutex") % name_).str(), file_, line_);
        lock_.lock();
        kaa_log_message(LoggerFactory::getLogger(), LogLevel::DEBUG, (boost::format("Locked %1% mutex") % name_).str(), file_, line_);
    }
    ~MutexScopedLockLogger() {
        kaa_log_message(LoggerFactory::getLogger(), LogLevel::DEBUG, (boost::format("Unlocking %1% mutex") % name_).str(), file_, line_);
    }
private:
    const char *name_;
    const char *file_;
    size_t      line_;
    LockType   lock_;
};

    #define KAA_MUTEX_LOCKING(mutex_name)   KAA_LOG_DEBUG("Locking " mutex_name " mutex");
    #define KAA_MUTEX_LOCKED(mutex_name)    KAA_LOG_DEBUG("Locked " mutex_name " mutex");
    #define KAA_MUTEX_UNLOCKING(mutex_name) KAA_LOG_DEBUG("Unlocking " mutex_name " mutex");
    #define KAA_MUTEX_UNLOCKED(mutex_name)  KAA_LOG_DEBUG("Unlocked " mutex_name " mutex");
    #define KAA_MUTEX_LOG_AND_LOCK(LockType, MutableType, MutableObject) MutexScopedLockLogger<LockType, MutableType> MutableObject##Lock(#MutableObject, MutableObject, __LOGFILE, __LINE__)
#else
    #define KAA_MUTEX_LOCKING(mutex_name)
    #define KAA_MUTEX_LOCKED(mutex_name)
    #define KAA_MUTEX_UNLOCKING(mutex_name)
    #define KAA_MUTEX_UNLOCKED(mutex_name)
    #define KAA_MUTEX_LOG_AND_LOCK(LockType, MutableType, MutableObject) LockType MutableObject##Lock(MutableObject)
#endif

}  // namespace kaa

#endif /* LOG_HPP_ */
