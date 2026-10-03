import koLocale from '@fullcalendar/core/locales/ko'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin from '@fullcalendar/interaction'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import { useCallback, useState } from 'react'
import { scheduleApi, typeColor } from '../../api/schedules'
import { toDateParam } from '../../utils/date'

/**
 * 월간/주간 캘린더. 보이는 날짜 범위가 바뀔 때마다 /api/schedules/calendar 로 일정을 가져온다.
 * reloadKey 가 바뀌면(등록/수정/삭제 후) 다시 조회한다.
 */
export default function ScheduleCalendar({ scope, reloadKey, onSelectRange, onEventClick }) {
  // 주간 화면은 높이를 고정해야 scrollTime(08:00)부터 보인다. 월간은 내용만큼 늘어난다.
  const [viewType, setViewType] = useState('dayGridMonth')

  const fetchEvents = useCallback(
    (info, success, failure) => {
      scheduleApi
        .calendar(toDateParam(info.start), toDateParam(info.end), scope)
        .then((items) =>
          success(
            items.map((s) => ({
              id: String(s.scheduleId),
              title: scope === 'ME' ? s.title : `[${s.userName}] ${s.title}`,
              start: s.startAt,
              end: s.endAt,
              backgroundColor: typeColor(s.type),
              borderColor: typeColor(s.type),
              classNames: s.status === 'CANCELLED' ? ['event-cancelled'] : [],
              extendedProps: { schedule: s },
            })),
          ),
        )
        .catch(failure)
    },
    // reloadKey 가 바뀌면 함수가 새로 만들어지고, FullCalendar 가 이벤트를 다시 불러온다
    // oxlint-disable-next-line react-hooks/exhaustive-deps
    [scope, reloadKey],
  )

  return (
    <div className="calendar-wrap">
      <FullCalendar
        plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
        locale={koLocale}
        initialView="dayGridMonth"
        headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek' }}
        height={viewType === 'timeGridWeek' ? 680 : 'auto'}
        datesSet={(info) => setViewType(info.view.type)}
        dayMaxEvents={3}
        nowIndicator
        selectable
        selectMirror
        events={fetchEvents}
        eventTimeFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
        slotLabelFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
        scrollTime="08:00:00"
        select={(info) => {
          info.view.calendar.unselect()
          onSelectRange(info.start, info.end, info.allDay)
        }}
        eventClick={(info) => onEventClick(info.event.extendedProps.schedule)}
      />
    </div>
  )
}
