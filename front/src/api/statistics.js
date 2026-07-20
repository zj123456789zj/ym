import request from './request'

export const getOverview = () => request.get('/statistics/overview')

export const getTrend = (startDate, endDate) =>
    request.get('/statistics/trend', { params: { startDate, endDate } })

export const getOverviewRange = (startDate, endDate) =>
    request.get('/statistics/overview/range', { params: { startDate, endDate } })
