import koLocale from '@fullcalendar/core/locales/ko'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin from '@fullcalendar/interaction'
import listPlugin from '@fullcalendar/list'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import { useCallback, useState } from 'react'
import { scheduleApi, typeColor } from '../../api/schedules'
import { toDateParam } from '../../utils/date'

// 폰처럼 좁은 화면에서는 월간 칸이 너무 작으므로 목록 보기로 시작한다
const isNarrow = () => window.matchMedia('(max-width: 640px)').matches

/**
 * 월간 / 주간 / 목록 캘린더. 보이는 날짜 범위가 바뀔 때마다 /api/schedules/calendar 로 일정을 가져온다.
 * 일정마다 시간·제목·장소를 캘린더 안에 바로 보여줘서 하나씩 눌러보지 않아도 되게 한다.
 * reloadKey 가 바뀌면(등록/수정/삭제 후) 다시 조회한다.
 */
export default function ScheduleCalendar({ scope, reloadKey, onSelectRange, onEventClick }) {
  const [initialView] = useState(() => (isNarrow() ? 'listMonth' : 'dayGridMonth'))
  // 주간 화면은 높이를 고정해야 scrollTime(08:00)부터 보인다. 월간/목록은 내용만큼 늘어난다.
  const [viewType, setViewType] = useState(initialView)

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
        plugins={[dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin]}
        locale={koLocale}
        initialView={initialView}
        headerToolbar={
          isNarrow()
            ? { left: 'prev,next', center: 'title', right: 'listMonth,dayGridMonth' }
            : { left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,listMonth' }
        }
        buttonText={{ listMonth: '목록' }}
        height={viewType === 'timeGridWeek' ? 680 : 'auto'}
        datesSet={(info) => setViewType(info.view.type)}
        dayMaxEvents={false}
        eventDisplay="block"
        eventContent={renderEvent}
        noEventsContent="이 기간에 일정이 없습니다."
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

/** 캘린더 안에 시간 · 제목 · 장소(· 출장)를 모두 표시 */
function renderEvent(arg) {
  const s = arg.event.extendedProps.schedule
  if (arg.view.type.startsWith('list')) {
    return (
      <span className="ev-list">
        <strong>{arg.event.title}</strong>
        {s.location && <span className="ev-loc"> · {s.location}</span>}
        {s.tripTitle && <span className="tag">{s.tripTitle}</span>}
        <span className="ev-status">{s.status !== 'PLANNED' && s.statusLabel}</span>
      </span>
    )
  }
  return (
    <div className="ev">
      {arg.timeText && <span className="ev-time">{arg.timeText}</span>}
      <span className="ev-title">{arg.event.title}</span>
      {s.location && <span className="ev-loc">{s.location}</span>}
    </div>
  )
}
